package com.project.wms.interfaces.rest.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [Utf8ByteSizeValidator::class])
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Utf8ByteSize(
    val min: Int = 0,
    val max: Int = Int.MAX_VALUE,
    val message: String = "UTF-8 바이트 길이가 허용 범위를 벗어났습니다",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class Utf8ByteSizeValidator : ConstraintValidator<Utf8ByteSize, String> {
    private var min: Int = 0
    private var max: Int = Int.MAX_VALUE

    override fun initialize(annotation: Utf8ByteSize) {
        min = annotation.min
        max = annotation.max
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true
        val size = value.toByteArray(Charsets.UTF_8).size
        return size in min..max
    }
}
