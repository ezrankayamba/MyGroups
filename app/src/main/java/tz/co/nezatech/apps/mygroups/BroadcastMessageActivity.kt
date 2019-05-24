package tz.co.nezatech.apps.mygroups

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_boadcast_message.*
import kotlinx.android.synthetic.main.content_boadcast_message.*


const val PERMISSIONS_REQUEST_SEND_SMS = 101

class BroadcastMessageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boadcast_message)
        setSupportActionBar(toolbar)


        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val groupId = intent.getIntExtra("EXTRA_GROUP_ID", 0)
        val groupTitle = intent.getStringExtra("EXTRA_GROUP_TITLE")

        actionBarSetup(groupTitle, "Sending broadcast message")

        val execute = LoadContactsTask(this, groupId).execute()
        val contacts = execute.get()
        fab.setOnClickListener { view ->
            broadcast(contacts)
        }
    }

    private fun broadcast(contacts: ArrayList<MyContact>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.SEND_SMS), PERMISSIONS_REQUEST_SEND_SMS)
        } else {
            BroadcastTask(this, contacts, message.text.toString(), excludes.text.toString()).execute()
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun actionBarSetup(title: String, subTitle: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            val ab = supportActionBar
            ab!!.title = title
            ab.subtitle = subTitle
        }
    }

    class BroadcastTask(
        val context: Activity,
        private val contacts: ArrayList<MyContact>,
        private val message: String,
        private val excludes: String
    ) : AsyncTask<Void, Void, String>() {
        override fun doInBackground(vararg params: Void?): String {
            Log.d(TAG, "Size: ${contacts.size}")
            val smgr = SmsManager.getDefault()
            var msg = message
            var exl = excludes
            var excludes = ArrayList<String>()
            exl.split(",").forEach {
                var p = it.replace(" ", "")
                if (p.isNotEmpty()) {
                    p = p.substring(p.length - 9)
                    excludes.add(p)
                }
            }
            Log.d(TAG, "Excludes: $excludes")

            contacts.forEach {
                var phone = it.phone.toString()
                if (phone != null) {
                    phone = phone.replace(" ", "")
                    phone = phone.substring(phone.length - 9)
                    Log.d(TAG, "Sending to $phone")
                    if (!excludes.contains(phone)) {
                        phone = "+255$phone"
                        val parts = smgr.divideMessage(msg)
                        smgr.sendMultipartTextMessage(phone, null, parts, null, null)
                    } else {
                        Log.d(TAG, "Exclude: $phone")
                    }
                }
            }
            return "Successfully sent messages to ${contacts.size} group members!"
        }

        override fun onPreExecute() {
            super.onPreExecute()
            context.findViewById<ProgressBar>(R.id.progress_bar).visibility = View.VISIBLE
        }

        override fun onPostExecute(msg: String) {
            super.onPostExecute(msg)
            Snackbar.make(context.findViewById<ProgressBar>(R.id.progress_bar), msg, Snackbar.LENGTH_LONG).show()
            context.findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
        }
    }

    class LoadContactsTask(val context: Activity, val groupId: Int) : AsyncTask<Void, Void, ArrayList<MyContact>>() {
        override fun doInBackground(vararg params: Void?): ArrayList<MyContact> {
            return members(groupId)
        }

        override fun onPreExecute() {
            super.onPreExecute()
            context.findViewById<ProgressBar>(R.id.progress_bar).visibility = View.VISIBLE
        }

        override fun onPostExecute(myDataset: ArrayList<MyContact>) {
            super.onPostExecute(myDataset)
            val viewManager = LinearLayoutManager(context)
            val viewAdapter = MyContactAdapter(myDataset)

            val recyclerView = context.findViewById<RecyclerView>(R.id.contacts).apply {
                setHasFixedSize(true)
                layoutManager = viewManager
                adapter = viewAdapter
            }
            val dividerItemDecoration = DividerItemDecoration(
                recyclerView.context,
                viewManager.getOrientation()
            )
            recyclerView.addItemDecoration(dividerItemDecoration)
            context.findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
        }

        fun members(groupId: Int): ArrayList<MyContact> {
            val groupURI = ContactsContract.Data.CONTENT_URI

            val projection = arrayOf(
                ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
                ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID,
                ContactsContract.Contacts.DISPLAY_NAME
            )

            val c = context.managedQuery(
                groupURI,
                projection,
                ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + "=" + groupId,
                null,
                ContactsContract.Contacts.DISPLAY_NAME
            )
            val myDataset = ArrayList<MyContact>()
            while (c.moveToNext()) {
                var name = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                var id = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID))

                val phoneFetchCursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.TYPE
                    ),
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + id, null, null
                )
                var phone = "Not set"
                //var name ="Not set"
                while (phoneFetchCursor!!.moveToNext()) {
                    phone =
                        phoneFetchCursor.getString(phoneFetchCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    break
                }

                myDataset.add(MyContact(id, name, phone))
            }
            return myDataset
        }
    }
}
