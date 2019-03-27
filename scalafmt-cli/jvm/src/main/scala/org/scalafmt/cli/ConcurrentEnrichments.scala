package org.scalafmt.cli

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator

object ConcurrentEnrichments {
  implicit class XtensionAtomicReference[T](ref: AtomicReference[T]) {
    def crossGetAndUpdate(op: UnaryOperator[T]): Unit =
      ref.getAndUpdate(op)
  }
}
