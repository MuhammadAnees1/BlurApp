package com.example.blureffectproject.main.adaptor
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.blureffectproject.databinding.ItemBlurButtonBinding
import com.example.blureffectproject.models.BlurModel

class BlurAdapter(
    private val context: Context,
    private val blurList: List<BlurModel>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<BlurAdapter.BlurViewHolder>() {

    inner class BlurViewHolder(val binding: ItemBlurButtonBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(blurModel: BlurModel) {
            binding.imageView.setImageResource(blurModel.imageResId)
            binding.text.text = blurModel.label

            binding.root.setOnClickListener {
                onItemClick(blurModel.imageResId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlurViewHolder {
        val binding = ItemBlurButtonBinding.inflate(LayoutInflater.from(context), parent, false)
        return BlurViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BlurViewHolder, position: Int) {
        holder.bind(blurList[position])
    }

    override fun getItemCount(): Int {
        return blurList.size
    }
}
