package main

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class PipelineTests(c: Component2, outputs: List[Int]) extends PeekPokeTester(c) {
  
  var i = 0
  do {
    val intCast = c.io.elements("out").asUInt
    if(outputs(i) != 0) {
      expect(intCast, outputs(i))
    }
    step(1)
    i += 1
  } while (i < outputs.length)

}

class PipelineTester extends ChiselFlatSpec {
  behavior of "Add Pipeline"

  val inputA = () => new DecoupledFifo(List(1, 2, 3), 5)
  val inputB = () => new DecoupledFifo(List(4, 5, 6), 5)
  val inputC = () => new DecoupledFifo(List(7, 8, 9), 5)
  val add = () => new DecoupledAdd

  val stage1 = () => new Component2(add, 1, Map("a" -> inputA, "b" -> inputA))
  val stage2 = () => new Component2(add, 2, Map("a" -> stage1, "b" -> inputB))
  val result = () => new Component2(add, 3, Map("a" -> stage2, "b" -> inputC))

  backends foreach {backend =>
    it should s"test the one-stage add circuit" in {
      Driver(stage1, backend)((c) => new PipelineTests(c, List(2, 4, 6))) should be (true)
    }
    it should s"test the two-stage add circuit" in {
      Driver(stage2, backend)((c) => new PipelineTests(c, List(0, 6, 9, 12))) should be (true)
    }
    it should s"test the three-stage add circuit" in {
      Driver(result, backend)((c) => new PipelineTests(c, List(0, 0, 13, 17, 21))) should be (true)
    }
  }
}
