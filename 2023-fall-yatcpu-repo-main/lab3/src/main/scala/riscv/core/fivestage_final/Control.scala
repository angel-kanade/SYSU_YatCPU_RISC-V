// Copyright 2021 Howard Lau
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

package riscv.core.fivestage_final

import chisel3._
import riscv.Parameters

class Control extends Module {
  val io = IO(new Bundle {
    val jump_flag = Input(Bool())
    val jump_instruction_id = Input(Bool())
    val rs1_id = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val rs2_id = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val memory_read_enable_ex = Input(Bool())
    val rd_ex = Input(UInt(Parameters.PhysicalRegisterAddrWidth))
    val memory_read_enable_mem = Input(Bool())
    val rd_mem = Input(UInt(Parameters.PhysicalRegisterAddrWidth))

    val if_flush = Output(Bool())
    val id_flush = Output(Bool())
    val pc_stall = Output(Bool())
    val if_stall = Output(Bool())
  })

//  ctrl.io.jump_flag := id.io.if_jump_flag
//  ctrl.io.jump_instruction_id := id.io.ctrl_jump_instruction
//  ctrl.io.rs1_id := id.io.regs_reg1_read_address
//  ctrl.io.rs2_id := id.io.regs_reg2_read_address
//  ctrl.io.memory_read_enable_ex := id2ex.io.output_memory_read_enable
//  ctrl.io.rd_ex := id2ex.io.output_regs_write_address
//  ctrl.io.memory_read_enable_mem := ex2mem.io.output_memory_read_enable
//  ctrl.io.rd_mem := ex2mem.io.output_regs_write_address

  // Lab3(Final)
  //考虑到分支跳转指令在id段便要完成取数计算 故要在以下条件下阻塞
  val hazard_flag = ((io.jump_instruction_id || io.memory_read_enable_ex) && io.rd_ex =/= 0.U && (io.rs1_id === io.rd_ex || io.rs2_id === io.rd_ex)) ||
                    ((io.memory_read_enable_mem && io.jump_instruction_id) && io.rd_mem =/= 0.U && (io.rs1_id === io.rd_mem || io.rs2_id === io.rd_mem))

  //此时分支判断在id段 故不能flush 要让其正常完成
  when(hazard_flag){
    io.if_flush := false.B
    io.id_flush := true.B
    io.pc_stall := true.B
    io.if_stall := true.B
  }.elsewhen((io.jump_flag)){
    io.if_flush := true.B
    io.id_flush := false.B
    io.pc_stall := false.B
    io.if_stall := false.B
  }.otherwise{
    io.if_flush := false.B
    io.id_flush := false.B
    io.pc_stall := false.B
    io.if_stall := false.B
  }
  // Lab3(Final) End
}
