package com.pluu.plugin.wizard.fragment

import com.android.tools.idea.wizard.template.Category
import com.android.tools.idea.wizard.template.CheckBoxWidget
import com.android.tools.idea.wizard.template.Constraint.CLASS
import com.android.tools.idea.wizard.template.Constraint.LAYOUT
import com.android.tools.idea.wizard.template.Constraint.NONEMPTY
import com.android.tools.idea.wizard.template.Constraint.UNIQUE
import com.android.tools.idea.wizard.template.EnumWidget
import com.android.tools.idea.wizard.template.FormFactor
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.PackageNameWidget
import com.android.tools.idea.wizard.template.Separator
import com.android.tools.idea.wizard.template.StringParameter
import com.android.tools.idea.wizard.template.TextFieldWidget
import com.android.tools.idea.wizard.template.WizardUiContext
import com.android.tools.idea.wizard.template.booleanParameter
import com.android.tools.idea.wizard.template.enumParameter
import com.android.tools.idea.wizard.template.fragmentToLayout
import com.android.tools.idea.wizard.template.impl.activities.common.MIN_API
import com.android.tools.idea.wizard.template.impl.defaultPackageNameParameter
import com.android.tools.idea.wizard.template.layoutToFragment
import com.android.tools.idea.wizard.template.stringParameter
import com.android.tools.idea.wizard.template.template
import com.pluu.plugin.PluuPlugin
import com.pluu.plugin.wizard.common.ViewBindingType
import com.pluu.plugin.wizard.common.viewmodel.viewToViewModel
import java.io.File

val sampleFragmentSetupTemplate
    get() = template {
        name = PluuPlugin.FRAGMENT_WITH_VIEWMODEL
        minApi = MIN_API
        description = "Creates a Fragment with a ViewModel"

        category = Category.Fragment
        formFactor = FormFactor.Mobile

        screens = listOf(
            WizardUiContext.FragmentGallery,
            WizardUiContext.MenuEntry
        )

        lateinit var fragmentClass: StringParameter
        val layoutName = stringParameter {
            name = "Layout Name"
            default = "fragment_blank"
            suggest = { fragmentToLayout(fragmentClass.value) }
            help = "The name of the layout to create for the fragment"
            constraints = listOf(LAYOUT, UNIQUE, NONEMPTY)
        }

        fragmentClass = stringParameter {
            name = "Fragment Name"
            default = "BlankFragment"
            suggest = { layoutToFragment(layoutName.value) }
            help = "The name of the fragment class to create"
            constraints = listOf(CLASS, UNIQUE, NONEMPTY)
        }

        val useBinding = enumParameter<ViewBindingType> {
            name = "Use binding"
            default = ViewBindingType.ViewBinding
            help = "Help"
        }

        val isViewModel = booleanParameter {
            name = "Use ViewModel"
            default = true
            help = "(Default true), Use ViewModel"
        }
        val isActivityViewModel = booleanParameter {
            name = "Use Shared ViewModel"
            visible = { isViewModel.value }
            default = false
            help = "If true, parent activity's view model class to create"
        }

        val viewModelClass = stringParameter {
            name = "ViewModel Name"
            default = "MainViewModel"
            suggest = { viewToViewModel(fragmentClass.value) }
            visible = { isViewModel.value }
            help = "The name of the view model class to create"
            constraints = listOf(CLASS, UNIQUE, NONEMPTY)
        }

        val packageNameParam = defaultPackageNameParameter

        widgets(
            TextFieldWidget(fragmentClass),
            TextFieldWidget(layoutName),
            CheckBoxWidget(isViewModel),
            TextFieldWidget(viewModelClass),
            CheckBoxWidget(isActivityViewModel),
            EnumWidget(useBinding),
            Separator,
            PackageNameWidget(packageNameParam)
        )

        thumb { File("blank-fragment").resolve("template_blank_fragment.png") }

        recipe = { data ->
            sampleFragmentSetup(
                moduleData = data as ModuleTemplateData,
                packageName = packageNameParam.value,
                fragmentClass = fragmentClass.value,
                layoutName = layoutName.value,
                isUsedViewModel = isViewModel.value,
                isUsedSharedViewModel = isActivityViewModel.value,
                viewModelClass = viewModelClass.value,
                viewBindingType = useBinding.value
            )
        }
    }
