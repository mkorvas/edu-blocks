// Runs an emitted-JavaScript file the way the in-app sandbox does
// (events.cljs), with the command helpers of fs.cljc backed by the
// real filesystem. Usage: node harness.js SCRIPT.js
const fs = require('fs');

// clojure.string/split-lines: split on \r?\n, dropping trailing empties.
function splitLines(text) {
  const lines = text.split(/\r?\n/);
  while (lines.length && lines[lines.length - 1] === '') lines.pop();
  return lines;
}
function linesToText(lines) {
  return lines.length ? lines.join('\n') + '\n' : '';
}

function cat(path) {
  return fs.readFileSync(path, 'utf8');
}
function head(n, text) {
  if (text === '') return '';
  return linesToText(splitLines(text).slice(0, n));
}
function tail(n, text) {
  if (text === '') return '';
  const lines = splitLines(text);
  return linesToText(lines.slice(Math.max(0, lines.length - n)));
}
function grep(pattern, text) {
  const re = new RegExp(pattern);
  return linesToText(splitLines(text).filter((l) => re.test(l)));
}
function wcl(text) {
  return String((text.match(/\n/g) || []).length) + '\n';
}
function sort(text) {
  if (text === '') return '';
  return linesToText(splitLines(text).sort());
}
function uniq(text) {
  if (text === '') return '';
  return linesToText(splitLines(text).filter((l, i, ls) => i === 0 || l !== ls[i - 1]));
}
function seq(from, to) {
  const out = [];
  for (let i = from; i <= to; i++) out.push(i);
  return linesToText(out);
}

const bufs = [''];
const append = (s) => { bufs[bufs.length - 1] += s; };
const sandboxConsole = { log: (x) => append(String(x) + '\n') };
const sandboxProcess = { stdout: { write: (x) => append(x) } };
const capture = (f) => {
  bufs.push('');
  try {
    f();
    return bufs[bufs.length - 1];
  } finally {
    bufs.pop();
  }
};

const code = fs.readFileSync(process.argv[2], 'utf8');
new Function('console', 'process', 'cat', 'head', 'tail', 'grep',
             'wcl', 'sort', 'uniq', 'seq', 'capture', code)(
  sandboxConsole, sandboxProcess, cat, head, tail, grep, wcl, sort, uniq, seq, capture);
process.stdout.write(bufs[0]);
