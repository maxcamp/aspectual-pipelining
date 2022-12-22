package main

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import CoolSyntax._

class PipelineTests(c: Component, outputs: List[Int]) extends PeekPokeTester(c) {

  var i = 0
  do {
    val intCast = c.io.elements("out").asUInt
    if(outputs(i) != -1) {
      expect(intCast, outputs(i))
    }
    step(1)
    i += 1
  } while (i < outputs.length)

}

class PipelineTests2(c: Component2, outputs: List[Int]) extends PeekPokeTester(c) {

  var i = 0
  do {
    val intCast = c.io.elements("out").asUInt
    poke(c.io.outReady, true)
    poke(c.io.inValid, true)
    if(outputs(i) != -1) {
      expect(intCast, outputs(i))
    }
    step(1)
    i += 1
  } while (i < outputs.length)

}

class PipelineTester extends ChiselFlatSpec {
  
  behavior of "Add Pipeline"

  val queueA = () => new Fifo(1, 2, 3, 4, 5, 6)
  val queueB = () => new Fifo(1, 2, 3, 4, 5, 6)
  val queueC = () => new Fifo(0, 1, 2, 3, 4, 5, 6)
  val queueD = () => new Fifo(0, 0, 1, 2, 3, 4, 5, 6)

  val addGen = () => new Add

  val stageOne = addGen(1, Map("a" -> queueA, "b" -> queueB))
  val stageTwo = addGen(2, Map("a" -> stageOne, "b" -> queueC))
  val result   = addGen(3, Map("a" -> stageTwo, "b" -> queueD))

  backends foreach {backend =>
    it should s"test the basic add pipeline" in {
      Driver(result, backend)((c) => new PipelineTests(c, List(-1, -1, 4, 8, 12, 16, 20, 24))) should be (true)
    }
  }



  val differentResult = addGen(3, Map("a" -> stageTwo, "b" -> stageOne))

  backends foreach {backend =>
    it should s"test the feed-forward add pipeline" in {
      Driver(differentResult, backend)((c) => new PipelineTests(c, List(-1, -1, 5, 10, 15, 20, 25, 30))) should be (true)
    }
  }





  behavior of "Ready/Valid Add Pipeline"

  val input = () => new DecoupledFifo(List(1, 2, 3, 4, 5, 6), 15)
  val decoupledAdd: () => DecoupledModule = () => new DecoupledAdd

  val stage1 = () => new Component2(decoupledAdd, 1, Map("a" -> input, "b" -> input))
  val stage2 = () => new Component2(decoupledAdd, 2, Map("a" -> stage1, "b" -> input))
  val stage3 = () => new Component2(decoupledAdd, 3, Map("a" -> stage2, "b" -> input))
  val stage4 = () => new Component2(decoupledAdd, 4, Map("a" -> stage3, "b" -> input))
  val stage5 = () => new Component2(decoupledAdd, 5, Map("a" -> stage4, "b" -> input))

  backends foreach {backend =>
    it should s"test the one-stage add circuit" in {
      Driver(stage1, backend)((c) => new PipelineTests2(c, List(2, 4, 6, 8, 10, 12))) should be (true)
    }
    it should s"test the two-stage add circuit" in {
      Driver(stage2, backend)((c) => new PipelineTests2(c, List(-1, 3, 6, 9, 12, 15, 18))) should be (true)
    }
    it should s"test the three-stage add circuit" in {
      Driver(stage3, backend)((c) => new PipelineTests2(c, List(-1, -1, 4, 8, 12, 16, 20, 24))) should be (true)
    }
    it should s"test the four-stage add circuit" in {
      Driver(stage4, backend)((c) => new PipelineTests2(c, List(-1, -1, -1, 5, 10, 15, 20, 25, 30))) should be (true)
    }
    it should s"test the five-stage add circuit" in {
      Driver(stage5, backend)((c) => new PipelineTests2(c, List(-1, -1, -1, -1, 6, 12, 18, 24, 30, 36))) should be (true)
    }
  }

  val stallOnIndex3 = () => new DecoupledFifo(List(1, 2, 3, 4, 5, 6), 3)
  val stallOnIndex4 = () => new DecoupledFifo(List(1, 2, 3, 4, 5, 6), 4)

  val stallingStage1_3 = () => new Component2(decoupledAdd, 1, Map("a" -> stallOnIndex3, "b" -> input))
  val normalStage2 = () => new Component2(decoupledAdd, 2, Map("a" -> stallingStage1_3, "b" -> input))
  val stallingStage3_4 = () => new Component2(decoupledAdd, 3, Map("a" -> normalStage2, "b" -> stallOnIndex4))

  backends foreach {backend =>
    it should s"test the pipeline with stalling queues 1" in {
      Driver(stallingStage3_4, backend)((c) => new PipelineTests2(c, List(-1, -1, 4, 8, 12, -1, -1, 16, -1, -1, 20, 24))) should be (true)
    }
  }

  // Generate VCD in test_run_dir
  // chisel3.iotesters.Driver.execute(Array("--generate-vcd-output", "on"), () => new Component2(add, 5, Map("a" -> stage4, "b" -> stage1))) { c =>
  //   new PipelineTests2(c, List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
  // } should be(true)
}
