package com.muhammadwahyudin.identitasku.ui.home

import android.os.Handler
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.google.android.material.snackbar.Snackbar
import com.muhammadwahyudin.identitasku.R
import com.muhammadwahyudin.identitasku.data.Constants
import com.muhammadwahyudin.identitasku.data.model.DataWithDataType
import com.muhammadwahyudin.identitasku.ui._helper.SwipeItemTouchHelper
import com.muhammadwahyudin.identitasku.utils.Commons
import org.jetbrains.anko.find

class HomeDataAdapter(data: List<DataWithDataType>) :
    BaseMultiItemQuickAdapter<DataWithDataType, BaseViewHolder>(data), SwipeItemTouchHelper.SwipeHelperAdapter {

    private var data_swiped = ArrayList<DataWithDataType>()
    private lateinit var aty: HomeActivity

    init {
        addItemType(Constants.TYPE_KTP, R.layout.item_home_data_list_ktp)
        addItemType(Constants.TYPE_REK_BANK, R.layout.item_home_data_list_rek_bank)
        addItemType(Constants.TYPE_DEFAULT, R.layout.item_home_data_list)
    }

    override fun getItemViewType(position: Int): Int {
        return data[position].itemType
    }

    override fun convert(helper: BaseViewHolder, item: DataWithDataType) {
        aty = mContext as HomeActivity
        // Edit
        helper.itemView.setOnClickListener {
            val bs = AddEditDataBottomSheet.newInstance(AddEditDataBottomSheet.EDIT, item)
            bs.show(aty.supportFragmentManager, bs.tag)
        }

        //Commons
        helper.setText(R.id.tv_data_type, item.typeName)
        helper.setText(R.id.tv_data_value, item.value)
        helper.setOnClickListener(R.id.btn_copy_value) {
            Commons.copyToClipboard(mContext, item.value, item.typeName)
        }

        when (item.itemType) {
            Constants.TYPE_KTP -> {


            }
            Constants.TYPE_REK_BANK -> {
                helper.setText(R.id.tv_data_bank, item.attr1)
            }
            Constants.TYPE_DEFAULT -> {

            }
        }

    }

    // SWIPE & DRAG

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                for (item in data_swiped) {
                    val index_removed = data.indexOf(item)
                    if (index_removed != -1) {
                        data.removeAt(index_removed)
                        notifyItemRemoved(index_removed)
                    }
                }
                data_swiped.clear()
                super.onScrollStateChanged(recyclerView, newState)
            }
        })
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onItemDismiss(position: Int) {
        val dataToDelete = data[position]
        data.removeAt(position)
        notifyItemRemoved(position)
        // clear handler callback first to avoid bug when swiping more than one data in short duration
        val deleteHandler = Handler() // should make this global

        // should add to list of datasToDelete

        // Handler to run data deletion on db after snackbar disappear
        deleteHandler.postDelayed({ aty.viewModel.deleteData(dataToDelete) }, 3500) // delete list of datas
        // Show snackbar with undo button
        Snackbar.make(aty.find(R.id.parent_home_activity), "${dataToDelete.typeName} deleted", Snackbar.LENGTH_LONG)
            .setAction("UNDO") {
                data.add(position, dataToDelete)
                notifyItemInserted(position)
                // cancel Handler
                deleteHandler.removeCallbacksAndMessages(null)
            }.show()

    }

//    fun onItemMove(fromPosition: Int, toPosition: Int) {
////        val tmp = data.removeAt(fromPosition)
////        data.add(if (toPosition > fromPosition) toPosition - 1 else toPosition, tmp)
//        Collections.swap(data, fromPosition, toPosition)
//        notifyItemMoved(fromPosition, toPosition)
//    }
}