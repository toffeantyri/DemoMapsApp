package ru.toffeantyri.demomapsapp.list_adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import ru.toffeantyri.demomapsapp.databinding.AddressItemBinding
import ru.toffeantyri.demomapsapp.model.PointAddressData

class AddressListAdapter(
    context: Context,
    resource: Int,
    private val list: List<PointAddressData>,
    private val itemClickInterface: ListItemClickInterface
) : ArrayAdapter<PointAddressData>(context, resource, list) {

    private val inflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = AddressItemBinding.inflate(inflater)
        binding.name.text = list[position].address
        binding.root.setOnClickListener {
            itemClickInterface.itemClick(position)
        }
        return binding.root
    }

}