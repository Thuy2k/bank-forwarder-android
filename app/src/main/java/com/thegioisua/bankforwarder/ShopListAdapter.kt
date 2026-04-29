package com.thegioisua.bankforwarder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ShopListAdapter(
    private val shops: MutableList<ShopConfig>,
    private val onEdit: (ShopConfig) -> Unit,
    private val onDelete: (ShopConfig) -> Unit
) : RecyclerView.Adapter<ShopListAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val displayNameView = itemView.findViewById<TextView>(android.R.id.text1)
        private val prefixView = itemView.findViewById<TextView>(android.R.id.text2)
        private val editBtn = itemView.findViewById<Button>(android.R.id.button1)
        private val deleteBtn = itemView.findViewById<Button>(android.R.id.button2)

        fun bind(shop: ShopConfig) {
            displayNameView?.text = shop.displayName
            prefixView?.text = "Prefix: ${shop.prefix} | Secret: ${if (shop.hmacSecret.isNotBlank()) "***" else "Trống"}"
            editBtn?.setOnClickListener { onEdit(shop) }
            deleteBtn?.setOnClickListener { onDelete(shop) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 12, 16, 12)

            addView(LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                addView(LinearLayout(parent.context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )

                    addView(TextView(parent.context).apply {
                        id = android.R.id.text1
                        textSize = 16f
                        setTextColor(parent.context.resources.getColor(android.R.color.black, null))
                    })

                    addView(TextView(parent.context).apply {
                        id = android.R.id.text2
                        textSize = 12f
                        setTextColor(parent.context.resources.getColor(android.R.color.darker_gray, null))
                        setPadding(0, 4, 0, 0)
                    })
                })

                addView(Button(parent.context).apply {
                    id = android.R.id.button1
                    text = "Sửa"
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginStart = 8 }
                })

                addView(Button(parent.context).apply {
                    id = android.R.id.button2
                    text = "Xóa"
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginStart = 8 }
                    setBackgroundColor(parent.context.resources.getColor(android.R.color.holo_red_light, null))
                })
            })
        }
        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(shops[position])
    }

    override fun getItemCount(): Int = shops.size

    fun updateList(newShops: List<ShopConfig>) {
        shops.clear()
        shops.addAll(newShops)
        notifyDataSetChanged()
    }
}
