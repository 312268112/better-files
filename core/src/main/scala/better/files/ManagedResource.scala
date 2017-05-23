package better.files

import java.util.concurrent.atomic.AtomicBoolean

import scala.util.Try

/**
  * A typeclass to denote a disposable resource
  * @tparam A
  */
trait Disposable[-A]  {
  def dispose(resource: A): Unit

  def disposeSilently(resource: A): Unit = {
    val _ = Try(dispose(resource))
  }
}

object Disposable {
  def apply[A](disposeMethod: A => Any): Disposable[A] = new Disposable[A] {
    override def dispose(resource: A) = {
      val _ = disposeMethod(resource)
    }
  }

  implicit val closableDisposer: Disposable[Closeable] =
    Disposable(_.close())

  val fileDisposer: Disposable[File] =
    Disposable(_.delete(swallowIOExceptions = true))
}

class ManagedResource[A](resource: A)(implicit disposer: Disposable[A]) {
  private[this] val isDisposed = new AtomicBoolean(false)

  def foreach[U](f: A => U): Unit = {
    val _ = map(f)
  }

  def map[B](f: A => B): B = {
    val result = Try(f(resource))
    if (!isDisposed.getAndSet(true)) disposer.disposeSilently(resource)
    result.get
  }
}
