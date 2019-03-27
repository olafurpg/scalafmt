package org.scalafmt.cli

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator

object ConcurrentEnrichments {
  implicit class XtensionAtomicReference[T <: AnyRef](ref: AtomicReference[T]) {
    def crossGetAndUpdate(op: UnaryOperator[T]): Unit =
      // TODO:
      // [error] cannot link: @java.util.concurrent.atomic.AtomicReference::set_java.lang.Object_unit
      () // ref.set(op.apply(ref.get()))
  }
}
