package com.muhammadwahyudin.identitasku.ui.home

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.muhammadwahyudin.identitasku.R
import com.muhammadwahyudin.identitasku.ui._base.BaseActivity
import kotlinx.android.synthetic.main.activity_home.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance

class HomeActivity : BaseActivity(), KodeinAware {
    override val kodein by closestKodein()
    private val viewModelFactory: HomeViewModelFactory by instance()
    lateinit var viewModel: HomeViewModel

    lateinit var dataAdapter: DataAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        dataAdapter = DataAdapter(this)
        initializeUI()
        initializeRecyclerView()
    }

    private fun initializeRecyclerView() {
        rv_data.layoutManager = LinearLayoutManager(this)
        rv_data.adapter = dataAdapter
        rv_data.itemAnimator = DefaultItemAnimator()

        rv_data.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && fab_add_data.visibility == View.VISIBLE) {
                    fab_add_data.hide()
                    actionBar?.hide()
                } else if (dy <= 0 && fab_add_data.visibility != View.VISIBLE) {
                    fab_add_data.show()
                    actionBar?.show()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (fab_add_data.visibility != View.VISIBLE) {
                    fab_add_data.show()
                    actionBar?.show()
                }
            }
        })
    }

    private fun initializeUI() {

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(HomeViewModel::class.java)
        viewModel.getAllDataWithType().observe(this, Observer { ListOfDataWithType ->
            dataAdapter.datasWithType = ListOfDataWithType
        })

        fab_add_data.setOnClickListener {
            val bsFragment = AddEditDataBottomSheet.newInstance(AddEditDataBottomSheet.ADD)
            bsFragment.show(supportFragmentManager, bsFragment.tag)
        }

        fab_add_data.setOnLongClickListener {
            viewModel.debugPopulateData()
            true
        }

    }
}
