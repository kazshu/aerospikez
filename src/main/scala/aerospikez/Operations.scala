package aerospikez

import com.aerospike.client.{ Bin, Operation }

trait Ops {

  def operate: Operation
}

object Operations {

  case class Prepend(value: String, binName: String = "") extends Ops {
    def operate = Operation.prepend(new Bin(binName, value))
  }

  case class Append(value: String, binName: String = "") extends Ops {
    def operate = Operation.append(new Bin(binName, value))
  }

  case class Put[V](value: V, val binName: String = "") extends Ops {
    def operate = Operation.put(new Bin(binName, value))
  }

  case class Add(value: Int, binName: String = "") extends Ops {
    def operate = Operation.add(new Bin(binName, value))
  }

  case class Get(val binName: String = "") extends Ops {
    def operate = Operation.get(binName)
  }

  case class GetHeader() extends Ops {
    def operate = Operation.getHeader()
  }

  case class Touch() extends Ops {
    def operate = Operation.touch()
  }
}
