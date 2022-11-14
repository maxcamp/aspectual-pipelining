package main
import chisel3._

object CoolSyntax {
    implicit class Function0Extension(f: Function0[Module]) {

        def apply(stage: Int, inputs: () => Module *): () => Component = {
            () => new Component(f, stage, inputs:_*)
        }

    }
}
