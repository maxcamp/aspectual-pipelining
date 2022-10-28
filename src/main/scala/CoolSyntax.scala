package main
import chisel3._

object CoolSyntax {
    implicit class Function0Extension(v: Function0[Module]) {

        def apply(x: () => Module *): () => Component = {
            () => new Component(v, x:_*)
        }

    }
}
