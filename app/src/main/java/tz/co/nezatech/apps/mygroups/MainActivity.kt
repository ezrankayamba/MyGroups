package tz.co.nezatech.apps.mygroups

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_main.*
import android.provider.ContactsContract
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.Manifest.permission.READ_CONTACTS
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.app.Activity
import android.content.Context
import android.os.AsyncTask
import android.provider.ContactsContract.CommonDataKinds
import android.view.View
import android.widget.ProgressBar


val GROUP_PROJECTION =
    arrayOf(ContactsContract.Groups._ID, ContactsContract.Groups.TITLE, ContactsContract.Groups.ACCOUNT_NAME)

class MainActivity : AppCompatActivity() {
    private val PERMISSIONS_REQUEST_READ_CONTACTS = 100
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        groups()
    }

    private fun groups() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(READ_CONTACTS), PERMISSIONS_REQUEST_READ_CONTACTS)
            return
        }

        LoadGroupsTask(this).execute()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                groups()
            } else {
                Toast.makeText(this, "Until you grant the permission, we canot display the names", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    class LoadGroupsTask(val context: Activity) : AsyncTask<Void, Void, ArrayList<MyGroup>>() {
        override fun doInBackground(vararg params: Void?): ArrayList<MyGroup> {
            val cursor = context.contentResolver.query(
                ContactsContract.Groups.CONTENT_URI, GROUP_PROJECTION,
                null, null, ContactsContract.Groups.TITLE
            )
            val myDataset = ArrayList<MyGroup>()
            var builtIns = arrayOf("Coworkers", "Family", "Friends", "ICE")
            while (cursor.moveToNext()) {

                val id = cursor.getInt(
                    cursor.getColumnIndex(ContactsContract.Groups._ID)
                )

                var gTitle = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.Groups.TITLE)
                )
                var accountName = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.Groups.ACCOUNT_NAME)
                )


                if (gTitle.contains("Starred in Android") || gTitle.contains("My Contacts") || builtIns.contains(gTitle)) {
                    continue
                }
                var count = members(id)
                //var count = fetchGroupMembers(id).size
                myDataset.add(MyGroup(id, gTitle, accountName, count))
            }
            return myDataset
        }

        override fun onPreExecute() {
            super.onPreExecute()
            context.findViewById<ProgressBar>(R.id.progress_bar).visibility = View.VISIBLE
        }

        override fun onPostExecute(myDataset: ArrayList<MyGroup>) {
            super.onPostExecute(myDataset)
            val viewManager = LinearLayoutManager(context)
            val viewAdapter = MyContactGroupAdapter(myDataset, context)

            val recyclerView = context.findViewById<RecyclerView>(R.id.contactGroups).apply {
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

        fun members(groupId: Int): Int {
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
            var i = 0;
            var j = 0;
            while (c.moveToNext()) {
                i++;
                var name = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                var id = c.getInt(c.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID))

                val phoneFetchCursor = context.contentResolver.query(
                    Phone.CONTENT_URI,
                    arrayOf(Phone.NUMBER, Phone.DISPLAY_NAME, Phone.TYPE),
                    Phone.CONTACT_ID + "=" + id, null, null
                )
                var phone = "Not set"
                //var name ="Not set"
                while (phoneFetchCursor!!.moveToNext()) {
                    j++;
                    phone = phoneFetchCursor.getString(phoneFetchCursor.getColumnIndex(Phone.NUMBER))
                    //name = phoneFetchCursor.getString(phoneFetchCursor.getColumnIndex(Phone.DISPLAY_NAME))
                    break
                }

                //var phonenumber = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID))
                //Log.d(TAG, "$name - $phone")
            }
            Log.d(TAG, "Count: ${i}/${j}")
            return c.count
        }
    }
}
