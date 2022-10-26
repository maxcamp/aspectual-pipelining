// See LICENSE.txt for license details.
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class AddTests(c: Add) extends PeekPokeTester(c) {
  val inputs = List( (48, 32), (7, 3), (100, 10) )
  val outputs = List(80, 10, 110)

  var i = 0
  do {
    poke(c.io.a, inputs(i)._1)
    poke(c.io.b, inputs(i)._2)
    step(1)
    expect(c.io.out, outputs(i))
    i += 1
  } while (i < 3)
}

class AddTester extends ChiselFlatSpec {
  behavior of "Add"

  backends foreach {backend =>
    it should s"test the basic add circuit" in {
      Driver(() => new Add, backend)((c) => new AddTests(c)) should be (true)
    }
  }
}


