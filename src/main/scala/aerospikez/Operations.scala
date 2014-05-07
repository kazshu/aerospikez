package aerospikez

import com.aerospike.client.{ Bin, Operation }

trait Ops {

  def toOperation: Operation
}

object Operations {

  case class Prepend(value: String, binName: String = "") extends Ops {
    def toOperation = Operation.prepend(new Bin(binName, value))
  }

  case class Append(value: String, binName: String = "") extends Ops {
    def toOperation = Operation.append(new Bin(binName, value))
  }

  case class Put[V](value: V, val binName: String = "") extends Ops {
    def toOperation = Operation.put(new Bin(binName, value))
  }

  case class Add(value: Int, binName: String = "") extends Ops {
    def toOperation = Operation.add(new Bin(binName, value))
  }

  case class Get(val binName: String = "") extends Ops {
    def toOperation = Operation.get(binName)
  }

  case class GetAll() extends Ops {
    def toOperation = Operation.get()
  }

  case class GetHeader() extends Ops {
    def toOperation = Operation.getHeader()
  }

  case class Touch() extends Ops {
    def toOperation = Operation.touch()
  }
}
