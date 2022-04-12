package io.agora.realtimemusicclass.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import io.agora.realtimemusicclass.R
import io.agora.realtimemusicclass.base.edu.classroom.ClassCreateRequest
import io.agora.realtimemusicclass.base.edu.classroom.ClassManager
import io.agora.realtimemusicclass.base.edu.core.data.RMCCallback
import io.agora.realtimemusicclass.base.edu.core.data.RMCClassInfo
import io.agora.realtimemusicclass.base.edu.core.data.RMCError
import io.agora.realtimemusicclass.base.edu.core.data.RMCUserInfo
import io.agora.realtimemusicclass.base.ui.activities.BaseActivity
import io.agora.realtimemusicclass.base.utils.ToastUtil
import io.agora.realtimemusicclass.ui.RMCClass
import io.agora.realtimemusicclass.ui.view.MainPageAdapter
import io.agora.realtimemusicclass.ui.view.TextInputLimiter
import io.agora.realtimemusicclass.ui.view.TextLimiterListener
import io.agora.realtimemusicclass.ui.view.default

class RoomCreateFragment(adapter: MainPageAdapter,
                         activity: BaseActivity,
                         @LayoutRes private val layout: Int) : AbsFragment(adapter, activity) {
    private var usePassword = false
    private lateinit var roomEdit: AppCompatEditText
    private lateinit var pwdEdit: AppCompatEditText
    private lateinit var pwdTitle: AppCompatTextView
    private lateinit var pwdSwitch: RelativeLayout

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

    private fun initLayout(layout: View) {
        layout.findViewById<AppCompatImageView>(R.id.fragment_room_create_back)?.setOnClickListener {
            getAdapter().gotoPrevPage()
        }

        roomEdit = layout.findViewById(R.id.fragment_room_create_name_edit)
        pwdEdit = layout.findViewById(R.id.fragment_fragment_room_create_password_edit)
        pwdEdit.addTextChangedListener(TextInputLimiter(default,
            object : TextLimiterListener {
                override fun onCharCountExceed(original: String?,
                                               after: String, start: Int,
                                               before: Int, count: Int) {
                    pwdEdit.setText(original)
                    pwdEdit.setSelection(start)
                    ToastUtil.toast(getBaseActivity(),
                        R.string.fragment_room_create_password_too_long)
                }
            }))

        setAlignment(layout)

        pwdSwitch = layout.findViewById(R.id.fragment_room_create_password_layout)
        pwdSwitch.isActivated = usePassword
        pwdTitle = layout.findViewById(R.id.fragment_room_create_password)

        setPasswordEnabled(usePassword)

        pwdSwitch.setOnClickListener {
            pwdSwitch.isActivated = !pwdSwitch.isActivated
            setPasswordEnabled(pwdSwitch.isActivated)
        }

        layout.findViewById<AppCompatTextView>(R.id.fragment_role_type_button)?.setOnClickListener {
            checkAndCreate()
        }
    }

    /**
     * Align two edit's left edges if their
     * leading names' lengths are not the same
     */
    private fun setAlignment(layout: View) {
        val nameHint: AppCompatTextView = layout.findViewById(R.id.fragment_room_create_name)
        val pwdHint: AppCompatTextView = layout.findViewById(R.id.fragment_room_create_password)
        nameHint.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        pwdHint.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val target: View
        val ref: View
        if (nameHint.measuredWidth >= pwdHint.measuredWidth) {
            target = pwdEdit
            ref = roomEdit
        } else {
            target = roomEdit
            ref = pwdEdit
        }
        alignStart(target, ref)
    }

    private fun alignStart(target: View, reference: View) {
        val params = target.layoutParams as? RelativeLayout.LayoutParams
        params?.let {
            it.removeRule(RelativeLayout.END_OF)
            it.removeRule(RelativeLayout.RIGHT_OF)
            it.leftMargin = 0
            it.addRule(RelativeLayout.ALIGN_START, reference.id)
        }
    }

    private fun setPasswordEnabled(enabled: Boolean) {
        usePassword = enabled
        pwdTitle.isEnabled = enabled
        pwdEdit.isEnabled = enabled
        if (!enabled) pwdEdit.setText("")
    }

    private fun checkAndCreate() {
        val name = roomEdit.text.toString()
        if (name.isEmpty()) {
            ToastUtil.toast(getBaseActivity(),
                R.string.fragment_room_create_empty_name)
            return
        }

        var pass: String? = null
        if (usePassword) {
            pass = pwdEdit.text.toString()
            if (pass.isNullOrEmpty()) {
                ToastUtil.toast(getBaseActivity(),
                    R.string.fragment_room_create_password_hint)
                return
            }
        }

        callCreateRoom(name, pass)
    }

    private fun callCreateRoom(roomName: String, pwd: String? = null) {
        getBaseActivity().showLoading()

        val userName = getAdapter().getLocalUserName()
        val req = ClassCreateRequest(
            roomName, userName, pwd)

        ClassManager.createClass(req, object : RMCCallback<RMCClassInfo> {
            override fun onSuccess(res: RMCClassInfo?) {
                res?.let { classInfo ->
                    ToastUtil.toast(getBaseActivity(), R.string.fragment_room_create_success)
                    roomEdit.setText("")
                    pwdEdit.setText("")
                    setPasswordEnabled(false)
                    pwdSwitch.isActivated = false

                    val info = RoomListInfoItem(
                        classInfo.className,
                        classInfo.creator,
                        classInfo.channelID,
                        classInfo.hasPasswd,
                        classInfo.count)
                    preCheck(info, pwd)
                }
            }

            override fun onFailure(error: RMCError) {
                ToastUtil.toast(getBaseActivity(), error.msg ?: "")
                getBaseActivity().dismissLoadingDialog()
            }
        })
    }

    private fun preCheck(classInfo: RoomListInfoItem, password: String? = null) {
        ClassManager.preClassCheck(classInfo.name, password, RMCUserInfo(
            getAdapter().getLocalUserName(), getAdapter().getSelectedRoleType(),
            null, 0, null), object : RMCCallback<RMCUserInfo> {
            override fun onSuccess(res: RMCUserInfo?) {
                res?.let {
                    getBaseActivity().dismissLoadingDialog()
                    getAdapter().reset()

                    RMCClass.launch(getBaseActivity(),
                        getAdapter().getSelectedRoomType(),
                        classInfo.name, classInfo.channelId, it)
                }
            }

            override fun onFailure(error: RMCError) {
                ToastUtil.toast(this@RoomCreateFragment.getBaseActivity(),
                    "join class error: ${error.msg}")
                getBaseActivity().dismissLoadingDialog()
            }
        })
    }
}