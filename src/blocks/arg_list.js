import Blockly from 'blockly/core';
import '@blockly/block-dynamic-connection';

// "Extend" the plugin's dynamic_list_create block into a list of arg names.
const dynaBlock = Blockly.Blocks['dynamic_list_create'];
export const argListBlock = Object.assign({}, dynaBlock);

argListBlock.addFirstInput = function() {
  return this.appendValueInput('ADD0').appendField('args');
};

argListBlock.init = function() {
  // Enforce the ArgName check on every input, including sockets added
  // dynamically by onPendingConnection.
  this.superAppendValueInput = this.appendValueInput;
  this.appendValueInput = function(name) {
    return this.superAppendValueInput(name).setCheck('ArgName');
  };

  this.itemCount = this.minInputs;
  this.setStyle('list_blocks');
  this.addFirstInput();
  for (let i = 1; i < this.minInputs; i++) {
    this.appendValueInput(`ADD${i}`);
  }
  this.setOutput(true, 'ArgList');
};
