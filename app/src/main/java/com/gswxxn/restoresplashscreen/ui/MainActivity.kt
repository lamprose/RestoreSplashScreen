package com.gswxxn.restoresplashscreen.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.gswxxn.restoresplashscreen.BuildConfig
import com.gswxxn.restoresplashscreen.R
import com.gswxxn.restoresplashscreen.data.ConstValue.BACKGROUND_EXCEPT
import com.gswxxn.restoresplashscreen.data.ConstValue.BRANDING_IMAGE
import com.gswxxn.restoresplashscreen.data.ConstValue.CUSTOM_SCOPE
import com.gswxxn.restoresplashscreen.data.ConstValue.DEFAULT_STYLE
import com.gswxxn.restoresplashscreen.data.ConstValue.EXTRA_MESSAGE
import com.gswxxn.restoresplashscreen.data.DataConst
import com.gswxxn.restoresplashscreen.databinding.ActivityMainBinding
import com.gswxxn.restoresplashscreen.utils.IconPackManager
import com.highcapable.yukihookapi.hook.factory.isXposedModuleActive
import com.highcapable.yukihookapi.hook.factory.modulePrefs
import com.highcapable.yukihookapi.hook.xposed.YukiHookModuleStatus
import com.topjohnwu.superuser.Shell

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object {
        lateinit var appContext: Context
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appContext = applicationContext

        binding.apply {

            mainTextVersion.text = "模块版本：${BuildConfig.VERSION_NAME}"

            // 重启UI
            titleRestartIcon.setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("重启SystemUI")
                    .setMessage("你真的要重启系统界面吗？")
                    .setPositiveButton("确定") { _, _ ->
                        Shell.cmd(
                            "pkill -f com.android.systemui",
                            "pkill -f com.gswxxn.restoresplashscreen"
                        ).exec()
                    }
                    .setNegativeButton("取消") { _, _ -> }
                    .show()
            }

            titleAboutPage.setOnClickListener {
                val intent = Intent(this@MainActivity, AboutPageActivity::class.java)
                startActivity(intent)
            }

            // 启用模块
            enableModel.apply {
                isChecked = modulePrefs.get(DataConst.ENABLE_MODULE)
                setOnCheckedChangeListener { _, isChecked ->
                    modulePrefs.put(DataConst.ENABLE_MODULE, isChecked)
                }
            }

            // 启用日志
            enableLog.apply {
                setOnCheckedChangeListener { _, isChecked ->
                    modulePrefs.put(DataConst.ENABLE_LOG, isChecked)
                }
                isChecked = modulePrefs.get(DataConst.ENABLE_LOG)
            }

            // 自定义模块作用域
            customScope.apply {
                setOnCheckedChangeListener { _, isChecked ->
                    binding.customScopeLayout.visibility =
                        if (isChecked) View.VISIBLE else View.GONE
                    modulePrefs.put(DataConst.ENABLE_CUSTOM_SCOPE, isChecked)
                }
                isChecked = modulePrefs.get(DataConst.ENABLE_CUSTOM_SCOPE)
            }

            hideIconInLauncherSwitch.apply {
                setOnCheckedChangeListener { btn, b ->
                    if (btn.isPressed.not()) return@setOnCheckedChangeListener
                    modulePrefs.put(DataConst.ENABLE_HIDE_ICON, b)
                    packageManager.setComponentEnabledSetting(
                        ComponentName(this@MainActivity, "${BuildConfig.APPLICATION_ID}.Home"),
                        if (b) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
                isChecked = modulePrefs.get(DataConst.ENABLE_HIDE_ICON)
            }

            // 排除模式
            customScopeExceptMode.apply {
                setOnCheckedChangeListener { _, isChecked ->
                    binding.exceptModeStatusText.text = if (isChecked) "不会" else "仅会"
                    modulePrefs.put(DataConst.IS_CUSTOM_SCOPE_EXCEPTION_MODE, isChecked)
                }
                isChecked = modulePrefs.get(DataConst.IS_CUSTOM_SCOPE_EXCEPTION_MODE)
            }

            // 作用域应用列表
            customScopeList.setOnClickListener {
                val intent = Intent(this@MainActivity, ConfigAppsActivity::class.java)
                intent.putExtra(EXTRA_MESSAGE, CUSTOM_SCOPE)
                startActivity(intent)
            }

            // 缩小图标
            shrinkIcon.apply {
                setOnCheckedChangeListener { _, isChecked ->
                    modulePrefs.put(DataConst.ENABLE_SHRINK_ICON, isChecked)
                }
                isChecked = modulePrefs.get(DataConst.ENABLE_SHRINK_ICON)
            }

            // 替换获取图标方式
            replaceIcon.apply {
                setOnCheckedChangeListener { _, isChecked ->
                    modulePrefs.put(DataConst.ENABLE_REPLACE_ICON, isChecked)
                }
                isChecked = modulePrefs.get(DataConst.ENABLE_REPLACE_ICON)
            }

            // 使用图标包
            iconPackList.apply {
                val item = arrayListOf<String>()
                val availableIconPacks = IconPackManager(this@MainActivity).getAvailableIconPacks()
                availableIconPacks.forEach {
                    item.add(it.key)
                }
                adapter = ArrayAdapter(this@MainActivity, R.layout.spinner_item, item)
                    .apply { setDropDownViewResource(R.layout.spinner_dropdown) }

                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        modulePrefs.put(
                            DataConst.ICON_PACK_PACKAGE_NAME,
                            availableIconPacks.values.elementAt(position)
                        )
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                availableIconPacks.forEach {
                    if (it.value == modulePrefs.get(DataConst.ICON_PACK_PACKAGE_NAME)) {
                        setSelection(item.indexOf(it.key))
                    }
                }
            }

            // 忽略应用主动设置的图标
            defaultStyle.apply {
                setOnCheckedChangeListener { _, isChecked ->
                    binding.defaultStyleList.visibility = if (isChecked) View.VISIBLE else View.GONE
                    modulePrefs.put(DataConst.ENABLE_DEFAULT_STYLE, isChecked)
                }
                isChecked = modulePrefs.get(DataConst.ENABLE_DEFAULT_STYLE)
            }

            // 忽略应用主动设置的图标 应用列表
            defaultStyleList.setOnClickListener {
                val intent = Intent(this@MainActivity, ConfigAppsActivity::class.java)
                intent.putExtra(EXTRA_MESSAGE, DEFAULT_STYLE)
                startActivity(intent)
            }

            // 移除底部图片
            removeBrandingImage.apply {
                setOnCheckedChangeListener { _, isChecked ->
                    modulePrefs.put(DataConst.REMOVE_BRANDING_IMAGE, isChecked)
                    binding.removeBrandingImageList.visibility = if (isChecked) View.VISIBLE else View.GONE
                }
                isChecked = modulePrefs.get(DataConst.REMOVE_BRANDING_IMAGE)
            }

            // 移除底部图片 配置列表
            removeBrandingImageList.setOnClickListener {
                val intent = Intent(this@MainActivity, ConfigAppsActivity::class.java)
                intent.putExtra(EXTRA_MESSAGE, BRANDING_IMAGE)
                startActivity(intent)
            }

            // 设置微信背景为黑色
            independentColorWechat.apply {
                setOnCheckedChangeListener { _, isChecked ->
                    modulePrefs.put(DataConst.INDEPENDENT_COLOR_WECHAT, isChecked)
                }
                isChecked = modulePrefs.get(DataConst.INDEPENDENT_COLOR_WECHAT)
            }

            // 自适应背景颜色
            replaceBg.apply {
                setOnCheckedChangeListener { _, isChecked ->
                    modulePrefs.put(DataConst.ENABLE_CHANG_BG_COLOR, isChecked)
                    binding.bgExceptList.visibility = if (isChecked) View.VISIBLE else View.GONE
                }
                isChecked = modulePrefs.get(DataConst.ENABLE_CHANG_BG_COLOR)
            }

            // 自适应背景颜色排除列表
            bgExceptList.setOnClickListener {
                val intent = Intent(this@MainActivity, ConfigAppsActivity::class.java)
                intent.putExtra(EXTRA_MESSAGE, BACKGROUND_EXCEPT)
                startActivity(intent)
            }

            // 忽略深色模式
            ignoreDarkMode.apply {
                setOnCheckedChangeListener { _, isChecked ->
                    modulePrefs.put(DataConst.IGNORE_DARK_MODE, isChecked)
                }
                isChecked = modulePrefs.get(DataConst.IGNORE_DARK_MODE)
            }

            // 移除背景图片
            removeBgDrawable.apply {
                setOnCheckedChangeListener { _, isChecked ->
                    modulePrefs.put(DataConst.REMOVE_BG_DRAWABLE, isChecked)
                }
                isChecked = modulePrefs.get(DataConst.REMOVE_BG_DRAWABLE)
            }

        }
    }

    @SuppressLint("SetTextI18n")
    private fun refreshState() {
        binding.apply {
            mainLinStatus.setBackgroundResource(
                when {
                    isXposedModuleActive && modulePrefs.get(DataConst.ENABLE_MODULE)
                        .not() -> R.drawable.bg_yellow_round
                    isXposedModuleActive -> R.drawable.bg_green_round
                    else -> R.drawable.bg_dark_round
                }
            )
            mainImgStatus.setImageResource(
                when {
                    isXposedModuleActive -> R.drawable.ic_success
                    else -> R.drawable.ic_warn
                }
            )
            mainTextStatus.text =
                when {
                    isXposedModuleActive && !modulePrefs.get(DataConst.ENABLE_MODULE) -> "模块已停用"
                    isXposedModuleActive -> "模块已激活"
                    else -> "模块未激活"
                }
            mainTextApiWay.visibility = if (isXposedModuleActive) View.VISIBLE else View.GONE
            mainTextApiWay.text =
                "Activated by ${YukiHookModuleStatus.executorName} API ${YukiHookModuleStatus.executorVersion}"
        }

        window.statusBarColor = getColor(
            when {
                isXposedModuleActive && modulePrefs.get(DataConst.ENABLE_MODULE)
                    .not() -> R.color.yellow
                isXposedModuleActive -> R.color.green
                else -> R.color.gray
            }
        )
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }
}