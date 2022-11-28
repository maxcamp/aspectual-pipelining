package main

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class DecoupledAddTests(c: DecoupledAdd) extends PeekPokeTester(c) {
  val inputs = List( (0, 32), (7, 3), (100, 10) )
  val outputs = List(32, 10, 110)

  var i = 0
  do {
    poke(c.io.a, inputs(i)._1)
    poke(c.io.b, inputs(i)._2)
    poke(c.io.outReady, true)
    poke(c.io.inValid, true)
    step(1)
    expect(c.io.outReady, true)
    expect(c.io.outValid, true)
    expect(c.io.out, outputs(i))
    i += 1
  } while (i < 3)
}

class DecoupledAddTester extends ChiselFlatSpec {
  behavior of "DecoupledAdd"

  backends foreach {backend =>
    it should s"test the basic decoupled add circuit" in {
      Driver(() => new DecoupledAdd, backend)((c) => new DecoupledAddTests(c)) should be (true)
    }
  }
}
