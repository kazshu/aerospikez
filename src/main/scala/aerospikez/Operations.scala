package aerospikez

import com.aerospike.client.{ Operation, Bin ⇒ ABin }
import aerospikez.internal.util.TSafe.VRestriction

trait Ops {

  def toOperation: Operation
}

object Operations {

  case class prepend(binName: String, value: String) extends Ops {
    @inline def toOperation = Operation.prepend(new ABin(binName, value))
  }

  case class append(binName: String, value: String) extends Ops {
    @inline def toOperation = Operation.append(new ABin(binName, value))
  }

  case class put[V: VRestriction](binName: String, value: V) extends Ops {
    @inline def toOperation = Operation.put(new ABin(binName, value))
  }

  case class add(binName: String, value: Int) extends Ops {
    @inline def toOperation = Operation.add(new ABin(binName, value))
  }

  case class get(binName: String = "") extends Ops {
    @inline def toOperation = Operation.get(binName)
  }

  case class getAll() extends Ops {
    @inline def toOperation = Operation.get()
  }

  case class getHeader() extends Ops {
    @inline def toOperation = Operation.getHeader()
  }

  case class touch() extends Ops {
    @inline def toOperation = Operation.touch()
  }
}
