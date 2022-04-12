package io.agora.realtimemusicclass.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.util.rangeTo
import io.agora.realtimemusicclass.R
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserRole
import io.agora.realtimemusicclass.base.ui.activities.BaseActivity
import io.agora.realtimemusicclass.base.utils.ToastUtil
import io.agora.realtimemusicclass.ui.view.MainPageAdapter
import io.agora.realtimemusicclass.ui.view.RoleTypeItemView

class RoleTypeFragment(adapter: MainPageAdapter,
                       activity: BaseActivity,
                       @LayoutRes private val layout: Int) : AbsFragment(adapter, activity) {
    private lateinit var nameEdit: AppCompatEditText
    private val nameChecker = UserNameCharacterRangeChecker()

    private lateinit var teacherItem: RoleTypeItemView
    private lateinit var studentItem: RoleTypeItemView
    private lateinit var audienceItem: RoleTypeItemView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(layout, null)
        initLayout(layout)
        return layout
    }

    private fun initLayout(view: View) {
        view.findViewById<AppCompatImageView>(R.id.fragment_role_type_back)?.setOnClickListener {
            getAdapter().gotoPrevPage()
        }

        teacherItem = view.findViewById(R.id.fragment_role_type_item_teacher)
        studentItem = view.findViewById(R.id.fragment_role_type_item_student)
        audienceItem = view.findViewById(R.id.fragment_role_type_item_audience)

        setSelectedRoleUI()

        teacherItem.setOnClickListener {
            teacherItem.isSelected = true
            studentItem.isSelected = false
            audienceItem.isSelected = false
            getAdapter().setSelectedRole(RoleType.Teacher.value)
        }

        studentItem.setOnClickListener {
            teacherItem.isSelected = false
            studentItem.isSelected = true
            audienceItem.isSelected = false
            getAdapter().setSelectedRole(RoleType.Student.value)
        }

        audienceItem.setOnClickListener {
            teacherItem.isSelected = false
            studentItem.isSelected = false
            audienceItem.isSelected = true
            getAdapter().setSelectedRole(RoleType.Audience.value)
        }

        view.findViewById<AppCompatTextView>(R.id.fragment_role_type_button)?.setOnClickListener {
            startClass()
        }

        nameEdit = view.findViewById(R.id.fragment_role_type_name_edit)
        nameEdit.setText(getAdapter().getLocalUserName())

        nameEdit.addTextChangedListener(object : TextWatcher {
            private var beforeString: CharSequence? = null
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                beforeString = s.toString()
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                beforeString?.let { last ->
                    if (!nameChecker.isValidUserId(s, start, count)) {
                        Log.d("checker", "in valid characters found: " +
                                "${s.subSequence(start, start + count)}, last $last")
                        nameEdit.setText(last)
                        nameEdit.setSelection(start)
                        ToastUtil.toast(getBaseActivity(), R.string.fragment_role_toast_invalid_user_name)
                    }
                }
            }

            override fun afterTextChanged(s: Editable) {

            }
        })
    }

    private fun startClass() {
        val content = nameEdit.text.toString()
        if (content.isNotEmpty()) {
            getAdapter().setLocalUserName(content)
            getAdapter().gotoNextPage()
        } else {
            // Notify user an empty user name
        }
    }

    private fun setSelectedRoleUI() {
        teacherItem.isSelected = false
        studentItem.isSelected = false
        audienceItem.isSelected = false

        val roleType = getAdapter().getSelectedRoleType()
        if (roleType == RoleType.Teacher.value) {
            teacherItem.isSelected = true
        } else if (roleType == RoleType.Student.value) {
            studentItem.isSelected = true
        } else {
            audienceItem.isSelected = true
        }
    }

    override fun onResume() {
        super.onResume()
        setSelectedRoleUI()
        nameEdit.setText(getAdapter().getLocalUserName())
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

enum class RoleType(val value: Int) {
    Unknown(RMCUserRole.ROLE_TYPE_UNKNOWN.value),
    Teacher(RMCUserRole.ROLE_TYPE_TEACHER.value),
    Student(RMCUserRole.ROLE_TYPE_STUDENT.value),
    Audience(RMCUserRole.ROLE_TYPE_AUDIENCE.value);

    companion object {
        fun getRoleType(value: Int): RoleType {
            return when (value) {
                Teacher.value -> Teacher
                Student.value -> Student
                Audience.value -> Audience
                else -> Unknown
            }
        }
    }
}

class UserNameCharacterRangeChecker {
    private val lowers = 48 rangeTo 57
    private val uppers = 65 rangeTo 90
    private val digits = 97 rangeTo 122

    /**
     * Check the count number of characters from index start of
     * a sequence have all valid characters.
     */
    fun isValidUserId(s: CharSequence, start: Int, count: Int): Boolean {
        if (start >= s.length) {
            return true
        }

        val finish = (start + count - 1).coerceAtMost(s.length - 1)
        for (i in start .. finish) {
            if (!isValidCharacter(s[i])) {
                return false
            }
        }
        return true
    }

    private fun isValidCharacter(char: Char): Boolean {
        val value = char.code
        return value in lowers || value in uppers || value in digits
    }
}