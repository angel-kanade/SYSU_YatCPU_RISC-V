// Copyright 2022 Canbin Huang
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package riscv.core.fivestage_forward

import Chisel.MuxLookup
import chisel3._
import chisel3.util.MuxCase
import riscv.Parameters

object ForwardingType {
  val NoForward = 0.U(2.W)
  val ForwardFromMEM = 1.U(2.W)
  val ForwardFromWB = 2.U(2.W)
}

class Forwarding extends Module {
  val io = IO(new Bundle() {
    val rs1_ex = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val rs2_ex = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val rd_mem = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val reg_write_enable_mem = Input(Bool())
    val rd_wb = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val reg_write_enable_wb = Input(Bool())

    // Forwarding Type
    val reg1_forward_ex = Output(UInt(2.W))
    val reg2_forward_ex = Output(UInt(2.W))
  })

//  forwarding.io.rs1_ex := id2ex.io.output_regs_reg1_read_address
//  forwarding.io.rs2_ex := id2ex.io.output_regs_reg2_read_address
//  forwarding.io.rd_mem := ex2mem.io.output_regs_write_address
//  forwarding.io.reg_write_enable_mem := ex2mem.io.output_regs_write_enable
//  forwarding.io.rd_wb := mem2wb.io.output_regs_write_address
//  forwarding.io.reg_write_enable_wb := mem2wb.io.output_regs_write_enable

  // Lab3(Forward)
  //根据输入值确定ForwardType
  io.reg1_forward_ex := MuxCase(
    ForwardingType.NoForward,
    Seq(
      (io.reg_write_enable_mem && (io.rd_mem === io.rs1_ex) && (io.rd_mem =/= 0.U)) -> ForwardingType.ForwardFromMEM,
      (io.reg_write_enable_wb && (io.rd_wb === io.rs1_ex) && (io.rd_wb =/= 0.U)) -> ForwardingType.ForwardFromWB
    )
  )

  io.reg2_forward_ex := MuxCase(
    ForwardingType.NoForward,
    Seq(
      (io.reg_write_enable_mem && (io.rd_mem === io.rs2_ex) && (io.rd_mem =/= 0.U)) -> ForwardingType.ForwardFromMEM,
      (io.reg_write_enable_wb && (io.rd_wb === io.rs2_ex) && (io.rd_wb =/= 0.U)) -> ForwardingType.ForwardFromWB
    )
  )
  // Lab3(Forward) End
}
