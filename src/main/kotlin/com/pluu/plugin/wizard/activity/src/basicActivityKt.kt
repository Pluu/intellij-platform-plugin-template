package com.pluu.plugin.wizard.activity.src

import com.android.tools.idea.wizard.template.Language
import com.android.tools.idea.wizard.template.escapeKotlinIdentifier
import com.android.tools.idea.wizard.template.getMaterialComponentName
import com.android.tools.idea.wizard.template.impl.activities.common.importViewBindingClass
import com.android.tools.idea.wizard.template.impl.activities.common.layoutToViewBindingClass
import com.android.tools.idea.wizard.template.renderIf
import com.pluu.plugin.wizard.common.ViewBindingType
import com.pluu.plugin.wizard.common.importDataBinding
import com.pluu.plugin.wizard.common.viewmodel.importActivityViewModel

///////////////////////////////////////////////////////////////////////////
// Origin : https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:wizard/template-impl/src/com/android/tools/idea/wizard/template/impl/activities/basicActivity/src/basicActivityKt.kt
///////////////////////////////////////////////////////////////////////////

fun basicActivityKt(
    applicationPackage: String?,
    packageName: String,
    useAndroidX: Boolean,
    activityClass: String,
    layoutName: String,
    viewBindingType: ViewBindingType,
    isUsedViewModel: Boolean,
    viewModelClass: String
): String {
    val applicationPackageBlock = renderIf(applicationPackage != null) {
        "import $applicationPackage.R"
    }

    val contentViewBlock = if (viewBindingType.isViewBinding) {
        """
        binding = ${layoutToViewBindingClass(layoutName)}.inflate(layoutInflater)
        setContentView(binding.root)
        """
    } else if (viewBindingType.isDataBinding) {
        """
        binding = DataBindingUtil.setContentView(this, R.layout.$layoutName)
        binding.lifecycleOwner = this
        """
    } else {
        "setContentView(R.layout.$layoutName)"
    }

    return """
package ${escapeKotlinIdentifier(packageName)}

import android.os.Bundle
${importActivityViewModel(isUsedViewModel)}
import ${getMaterialComponentName("android.support.v7.app.AppCompatActivity", useAndroidX)}
${importDataBinding(viewBindingType.isDataBinding)}
$applicationPackageBlock
${importViewBindingClass(viewBindingType.isUseBinding, packageName, applicationPackage, layoutName, Language.Kotlin)}

class $activityClass : AppCompatActivity() {

${renderIf(viewBindingType.isUseBinding) {"""
    private lateinit var binding: ${layoutToViewBindingClass(layoutName)}        
"""}}
${renderIf(isUsedViewModel) {"""
    private val viewModel by viewModels<${viewModelClass}>()        
"""}}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        $contentViewBlock
        
        setUpViews()
        ${renderIf(isUsedViewModel) {
        """
        setUpObservers()
        """
        }}
    }
    
    private fun setUpViews() {
        // TODO: Implement the View 
    }
    
    ${renderIf(isUsedViewModel) {
    """
    private fun setUpObservers() {
        // TODO: Implement the ViewModel Observers 
    }      
    """
    }}
}
"""
}
