package main
import chisel3._

object CoolSyntax {
    implicit class Function0Extension(f: Function0[DecoupledModule]) {

        def apply(stage: Int, inputs: Map[String, () => DecoupledModule]): () => Component2 = {
            () => new Component2(f, stage, inputs)
        }

    }
}
