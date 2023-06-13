package com.pluu.wizard.common

enum class ViewBindingType(private val desc: String) {
    None("Unused Binding"),
    ViewBinding("Use, ViewBinding"),
    DataBinding("Use, DataBinding");

    val isUnusedBinding: Boolean get() = this == None

    val isViewBinding: Boolean get() = this == ViewBinding

    val isDataBinding: Boolean get() = this == DataBinding

    val isUseBinding: Boolean get() = isViewBinding || isDataBinding

    override fun toString(): String {
        return desc
    }
}
