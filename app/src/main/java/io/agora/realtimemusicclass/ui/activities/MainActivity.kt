package io.agora.realtimemusicclass.ui.activities

import android.os.Bundle
import io.agora.realtimemusicclass.base.ui.activities.BaseActivity
import io.agora.realtimemusicclass.databinding.ActivityMainBinding
import io.agora.realtimemusicclass.ui.view.MainPageAdapter

class MainActivity : BaseActivity() {
    private val tag = "MainActivity"

    private lateinit var binding: ActivityMainBinding

    private lateinit var adapter: MainPageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        adapter = MainPageAdapter(this, binding.mainPager)
        binding.mainPager.isUserInputEnabled = false
        binding.mainPager.adapter = adapter
        binding.mainPager.setCurrentItem(0, false)
    }

    override fun onBackPressed() {
        if (!adapter.gotoPrevPage()) {
            super.onBackPressed()
        }
    }
}