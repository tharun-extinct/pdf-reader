package com.tom_roush.pdfbox.pdmodel.interactive.annotation

import com.tom_roush.pdfbox.cos.COSName

class PDAnnotationInk : PDAnnotationMarkup() {
    init {
        cosObject.setName(COSName.SUBTYPE, SUB_TYPE_INK)
    }

    var inkList: List<FloatArray>
        get() = getInkList()?.toList().orEmpty()
        set(value) {
            setInkList(value.toTypedArray())
        }
}
