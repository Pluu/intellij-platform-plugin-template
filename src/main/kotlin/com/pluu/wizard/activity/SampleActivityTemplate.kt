package com.pluu.wizard.activity

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
import com.android.tools.idea.wizard.template.activityToLayout
import com.android.tools.idea.wizard.template.booleanParameter
import com.android.tools.idea.wizard.template.enumParameter
import com.android.tools.idea.wizard.template.impl.activities.common.MIN_API
import com.android.tools.idea.wizard.template.impl.defaultPackageNameParameter
import com.android.tools.idea.wizard.template.layoutToActivity
import com.android.tools.idea.wizard.template.stringParameter
import com.android.tools.idea.wizard.template.template
import com.pluu.wizard.common.ViewBindingType
import com.pluu.wizard.common.viewmodel.viewToViewModel
import java.io.File

val sampleActivitySetupTemplate
    get() = template {
        name = "[Pluu] Activity with ViewModel"
        minApi = MIN_API
        description = "Creates a Activity with a ViewModel"

        category = Category.Activity
        formFactor = FormFactor.Mobile

        screens = listOf(
            WizardUiContext.ActivityGallery,
            WizardUiContext.MenuEntry,
            WizardUiContext.NewProject,
            WizardUiContext.NewModule
        )

        lateinit var activityClass: StringParameter
        val layoutName = stringParameter {
            name = "Layout Name"
            default = "activity_main"
            suggest = { activityToLayout(activityClass.value) }
            help = "The name of the layout to create for the activity"
            constraints = listOf(LAYOUT, UNIQUE, NONEMPTY)
        }

        activityClass = stringParameter {
            name = "Activity Name"
            default = "MainActivity"
            suggest = { layoutToActivity(layoutName.value) }
            help = "The name of the activity class to create"
            constraints = listOf(CLASS, UNIQUE, NONEMPTY)
        }

        val useBinding = enumParameter<ViewBindingType> {
            name = "Use binding"
            default = ViewBindingType.None
            help = "Help"
        }

        val isViewModel = booleanParameter {
            name = "ViewModel 생성 여부"
            default = true
            help = "(기본값 true), ViewModel 생성 여부"
        }
        val viewModelClass = stringParameter {
            name = "ViewModel Name"
            default = "MainViewModel"
            constraints = listOf(CLASS, UNIQUE, NONEMPTY)
            visible = { isViewModel.value }
            help = "The name of the view model class to create"
            suggest = { viewToViewModel(activityClass.value) }
        }

        val packageNameParam = defaultPackageNameParameter

        widgets(
            TextFieldWidget(activityClass),
            TextFieldWidget(layoutName),
            CheckBoxWidget(isViewModel),
            TextFieldWidget(viewModelClass),
            EnumWidget(useBinding),
            Separator,
            PackageNameWidget(packageNameParam)
        )

        thumb { File("template_empty_activity.png") }

        recipe = { data ->
            sampleActivitySetup(
                moduleData = data as ModuleTemplateData,
                packageName = packageNameParam.value,
                activityClass = activityClass.value,
                layoutName = layoutName.value,
                isUsedViewModel = isViewModel.value,
                viewModelClass = viewModelClass.value,
                viewBindingType = useBinding.value
            )
        }
    }