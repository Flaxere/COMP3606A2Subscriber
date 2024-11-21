package jareddefour.example.comp3606a2.PublisherList


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import jareddefour.example.comp3606a2.MainActivity
import jareddefour.example.comp3606a2.Publisher.Publisher
import jareddefour.example.comp3606a2.R

class PublisherListAdapter(var comAct: MainActivity, context: Context, var publisherList:MutableList<Publisher>) : RecyclerView.Adapter<PublisherListAdapter.ViewHolder>() {


    class ViewHolder(var mainAct: MainActivity, itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{
        lateinit var buttonClickListener: OnItemClickListener
        val publisherID: TextView = itemView.findViewById(R.id.publisherID)
        val minimumSpeed: TextView = itemView.findViewById(R.id.minSpeedTV)
        val maximumSpeed: TextView = itemView.findViewById(R.id.maxSpeedtv)

        var button: Button = itemView.findViewById(R.id.questionButton)
     init{
            button.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            val publisherID: String = publisherID.text.toString()
            mainAct.openSummary(publisherID)
            mainAct.updateHeader(publisherID)
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.studentinformation, parent, false)
        return ViewHolder(comAct,view)


    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val publisher = publisherList[position]
        holder.publisherID.text = publisher.id

    }


    override fun getItemCount(): Int {
        return publisherList.size
    }

    fun addItemToEnd(publisher: Publisher){
        publisherList.add(publisher)
        notifyItemInserted(publisherList.size)

    }


}


