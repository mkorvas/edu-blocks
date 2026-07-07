import Blockly from 'blockly/core';
import '@blockly/block-dynamic-connection';

// "Extend" the plugin's dynamic_list_create block into a list of words.
const dynaBlock = Blockly.Blocks['dynamic_list_create'];
export const wordListBlock = Object.assign({}, dynaBlock);

wordListBlock.addFirstInput = function() {
  return this.appendValueInput('ADD0').appendField('words');
};

wordListBlock.init = function() {
  // Enforce the String check on every input, including the sockets added
  // dynamically by onPendingConnection.
  this.superAppendValueInput = this.appendValueInput;
  this.appendValueInput = function(name) {
    return this.superAppendValueInput(name).setCheck('String');
  };

  this.itemCount = this.minInputs;
  this.setStyle('list_blocks');
  this.addFirstInput();
  for (let i = 1; i < this.minInputs; i++) {
    this.appendValueInput(`ADD${i}`);
  }
  this.setOutput(true, 'WordList');
};
