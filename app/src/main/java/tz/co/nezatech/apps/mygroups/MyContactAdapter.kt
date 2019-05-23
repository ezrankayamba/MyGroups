package tz.co.nezatech.apps.mygroups

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView



class MyContactAdapter(private val myDataset: ArrayList<MyContact>) : RecyclerView.Adapter<MyContactAdapter.MyViewHolder>() {
    private val TAG = "MyContactAdapter"
    class MyViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyContactAdapter.MyViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_contact_row, parent, false) as LinearLayout

        return MyViewHolder(layout)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val contact = myDataset[position]
        holder.layout.findViewById<TextView>(R.id.title).text = "%s".format(contact.title)
        holder.layout.findViewById<TextView>(R.id.phone).text = contact.phone
        holder.layout.setOnClickListener({
            Log.d(TAG, "Selected: ${contact.title}")
        })
    }

    override fun getItemCount() = myDataset.size
}