package main

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class PipelineTests(c: Component) extends PeekPokeTester(c) {
  val inputs = List( (48, 32), (7, 3), (100, 10) )
  val outputs = List(80, 10, 110)

  var i = 0
  do {
    poke(c.io.elements("a0"), inputs(i)._1)
    poke(c.io.elements("a1"), inputs(i)._2)
    step(1)
    expect(c.io.elements("out"), outputs(i))
    i += 1
  } while (i < 3)
}

class PipelineTester extends ChiselFlatSpec {
  behavior of "Add"

  backends foreach {backend =>
    it should s"test the basic add circuit" in {
      Driver(() => new Component(new Add), backend)((c) => new PipelineTests(c)) should be (true)
    }
  }
}
