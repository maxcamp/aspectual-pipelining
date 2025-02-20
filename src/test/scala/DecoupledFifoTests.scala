package main

import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class DecoupledFifoTests(c: DecoupledFifo, stallOn: Int) extends PeekPokeTester(c) {

  var i = 1
  do {
    poke(c.io.outReady, true)
    expect(c.io.outValid, true)
    expect(c.io.out, i)
    step(1)
    i += 1
  } while (i < stallOn+1)

  // Stall for 2 clock ticks at a certain index
  poke(c.io.outReady, true)
  expect(c.io.outValid, false)
  step(2)

  do {
    poke(c.io.outReady, true)
    expect(c.io.outValid, true)
    expect(c.io.out, i)
    step(1)
    i += 1
  } while (i < 5)

}

class DecoupledFifoTester extends ChiselFlatSpec {
  behavior of "Decoupled Fifo"

  backends foreach {backend =>
    it should s"test the basic decoupled fifo circuit" in {
      val stallOn = 2
      Driver(() => new DecoupledFifo(List(1, 2, 3, 4), stallOn), backend)((c) => new DecoupledFifoTests(c, stallOn)) should be (true)
    }
  }
}
