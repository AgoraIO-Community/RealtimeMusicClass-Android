package io.agora.realtimemusicclass.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import io.agora.realtimemusicclass.R
import io.agora.realtimemusicclass.databinding.FragmentRoleTypeItemBinding

class RoleTypeItemView : FrameLayout {
    private lateinit var binding: FragmentRoleTypeItemBinding

    constructor(context: Context): super(context) {
        buildView(context)
    }

    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        buildView(context)
        setAttributes(context, attrs)
    }

    private fun buildView(context: Context) {
        binding = FragmentRoleTypeItemBinding.inflate(LayoutInflater.from(context))
        addView(binding.root)
    }

    private fun setAttributes(context: Context, attrs: AttributeSet) {
        val array = context.obtainStyledAttributes(attrs, R.styleable.RoleTypeItemView)
        val selected = array.getBoolean(R.styleable.RoleTypeItemView_selection, false)
        val name = array.getString(R.styleable.RoleTypeItemView_name)
        val img = array.getResourceId(R.styleable.RoleTypeItemView_img, R.drawable.role_type_img_student)
        array.recycle()

        binding.fragmentRoleTypeItemIcon.isActivated = selected
        binding.fragmentRoleTypeItemCheck.isVisible = selected
        binding.fragmentRoleTypeName.text = name
        binding.fragmentRoleTypeItemIcon.setImageResource(img)
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        binding.fragmentRoleTypeItemIcon.isActivated = selected
        binding.fragmentRoleTypeItemCheck.isVisible = selected
    }
}