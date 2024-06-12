package com.foodknalledge.reci_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CommentsAdapter(private val commentsList: List<RecipeView.Comment>) :
    RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fullNameTextView: TextView = itemView.findViewById(R.id.fullNameTextView)
        val commentTextView: TextView = itemView.findViewById(R.id.commentTextView)
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentsList[position]
        holder.fullNameTextView.text = "Full Name: ${comment.fullName}"
        holder.commentTextView.text = "Comment: ${comment.commentText}"
        holder.timestampTextView.text = "Timestamp: ${comment.timestampString}" // Format timestamp as needed
    }

    override fun getItemCount(): Int {
        return commentsList.size
    }
}
