package main

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class FifoTests(c: Fifo) extends PeekPokeTester(c) {

  var i = 1
  do {
    expect(c.io.out, i)
    step(1)
    i += 1
  } while (i < 5)
}

class FifoTester extends ChiselFlatSpec {
  behavior of "Fifo"

  backends foreach {backend =>
    it should s"test the basic fifo circuit" in {
      Driver(() => new Fifo(1, 2, 3, 4), backend)((c) => new FifoTests(c)) should be (true)
    }
  }
}
