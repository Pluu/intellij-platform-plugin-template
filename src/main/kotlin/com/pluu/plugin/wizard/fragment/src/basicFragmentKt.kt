package com.pluu.plugin.wizard.fragment.src

import com.android.tools.idea.wizard.template.Language
import com.android.tools.idea.wizard.template.escapeKotlinIdentifier
import com.android.tools.idea.wizard.template.getMaterialComponentName
import com.android.tools.idea.wizard.template.impl.activities.common.importViewBindingClass
import com.android.tools.idea.wizard.template.impl.activities.common.layoutToViewBindingClass
import com.android.tools.idea.wizard.template.renderIf
import com.pluu.plugin.wizard.common.ViewBindingType
import com.pluu.plugin.wizard.common.viewmodel.importFragmentViewModel

fun basicFragmentKt(
    applicationPackage: String?,
    packageName: String,
    useAndroidX: Boolean,
    fragmentClass: String,
    layoutName: String,
    viewBindingType: ViewBindingType,
    isUsedViewModel: Boolean,
    isUsedSharedViewModel: Boolean,
    viewModelClass: String
): String {
    val applicationPackageBlock = renderIf(applicationPackage != null) {
        "import $applicationPackage.R"
    }

    val createBindingBlock = if (viewBindingType.isViewBinding) {
        """
        private var _binding: ${layoutToViewBindingClass(layoutName)}? = null
        private val binding get() = _binding!!    
        """
    } else if (viewBindingType.isDataBinding) {
        "private lateinit var binding: ${layoutToViewBindingClass(layoutName)}"
    } else {
        ""
    }

    val createViewBlock = if (viewBindingType.isViewBinding) {
        """
        _binding = ${layoutToViewBindingClass(layoutName)}.inflate(layoutInflater, container, false)
        return binding.root
        """.trimIndent()
    } else if (viewBindingType.isDataBinding) {
        """
        binding = ${layoutToViewBindingClass(layoutName)}.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
        """.trimIndent()
    } else {
        "return inflater.inflate(R.layout.${layoutName}, container, false)"
    }

    return """
package ${escapeKotlinIdentifier(packageName)}

import android.os.Bundle
${importFragmentViewModel(isUsedViewModel, isUsedSharedViewModel)}
import ${getMaterialComponentName("android.support.v4.app.Fragment", useAndroidX)}
${importViewBindingClass(viewBindingType.isUseBinding, packageName, applicationPackage, layoutName, Language.Kotlin)}
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
$applicationPackageBlock

class $fragmentClass : Fragment() {

    ${renderIf(viewBindingType.isUseBinding) {
    """
    $createBindingBlock
    """
    }}
  
    ${renderIf(isUsedViewModel) {
    if (!isUsedSharedViewModel) {
        "private val viewModel by viewModels<${viewModelClass}>()"
    } else {
        "private val viewModel by activityViewModels<${viewModelClass}>()"
    }
    }}

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View${renderIf(viewBindingType.isUnusedBinding) { "?" }} {
        $createViewBlock
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        setUpViews()
        ${renderIf(isUsedViewModel) {
        "setUpObservers()"
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
    
    ${renderIf(viewBindingType.isViewBinding) {
    """
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }    
    """    
    }}
}
"""
}
