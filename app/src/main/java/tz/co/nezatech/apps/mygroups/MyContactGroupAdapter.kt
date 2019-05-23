package tz.co.nezatech.apps.mygroups

import android.app.Activity
import android.content.BroadcastReceiver
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat.startActivity
import android.content.Intent



const val TAG = "MyContactGroupAdapter"

class MyContactGroupAdapter(private val myDataset: ArrayList<MyGroup>, val context: Activity) : RecyclerView.Adapter<MyContactGroupAdapter.MyViewHolder>() {

    class MyViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyContactGroupAdapter.MyViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_group_row, parent, false) as LinearLayout

        return MyViewHolder(layout)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val group = myDataset[position]
        holder.layout.findViewById<TextView>(R.id.title).text = "%s (%s)".format(group.title, group.count)
        holder.layout.findViewById<TextView>(R.id.accountName).text = group.accountName
        holder.layout.setOnClickListener {
            Log.d(TAG, "Selected: ${group.title}")
            val intent = Intent(context, BoadcastMessageActivity::class.java)
            intent.putExtra("EXTRA_GROUP_ID", group.id)
            intent.putExtra("EXTRA_GROUP_TITLE", group.title)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = myDataset.size
}