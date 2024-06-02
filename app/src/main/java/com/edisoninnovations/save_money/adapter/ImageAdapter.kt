import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.edisoninnovations.save_money.R

class ImageAdapter(
    private val imageUris: ArrayList<Uri>,
    private val context: Context,
    private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(uri: Uri)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUri = imageUris[position]
        holder.imageView.setImageURI(imageUri)

        holder.imageView.setOnClickListener {
            itemClickListener.onItemClick(imageUri)
        }

        holder.deleteButton.setOnClickListener {
            imageUris.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, imageUris.size)
        }
    }

    override fun getItemCount(): Int {
        return imageUris.size
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image_view)
        val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)
    }
}
