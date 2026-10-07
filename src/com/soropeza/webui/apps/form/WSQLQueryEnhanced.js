/*
 * SQL Query Enhanced - client side support for the SQL editor.
 *
 * Upgrades the editor textarea to CodeMirror (syntax highlighting, line numbers). CodeMirror is
 * bundled in the plugin and sent by the server the first time an editor needs it (once per page);
 * until then, or when it is disabled, the plain textarea works with the same keyboard shortcuts.
 * Every execution request is sent to the server as one custom event carrying the selected text.
 */
(function () {
	if (window.SQLQueryEnhanced)
		return;

	var INDENT = '    ';
	var STYLE_ID = 'sqlq-enhanced-style';
	var STYLE = '.sqlq-editor.CodeMirror { width: 100%; height: 100%; box-sizing: border-box;'
		+ ' font-family: monospace; font-size: 13px; border: 1px solid #cfcfcf; border-radius: 2px; }'
		// placeholder needs this specificity to win over CodeMirror's ".CodeMirror pre.CodeMirror-line-like"
		+ ' .sqlq-editor.CodeMirror pre.CodeMirror-placeholder { color: #A0A0A0; font-style: italic; }';
	/** Editors waiting for the library, attached when the server sends it. */
	var pending = [];
	var requested = false;

	function addStyle(id, css) {
		if (document.getElementById(id))
			return;
		var style = document.createElement('style');
		style.id = id;
		style.textContent = css;
		document.head.appendChild(style);
	}

	function isLoaded() {
		return !!(window.CodeMirror && CodeMirror.modes && CodeMirror.modes.sql);
	}

	/** Called by the script the server sends with the bundled CodeMirror library. */
	function libLoaded(css) {
		addStyle(STYLE_ID + '-lib', css);
		addStyle(STYLE_ID, STYLE);
		var editors = pending;
		pending = [];
		editors.forEach(function (p) { attachCodeMirror(p.ed, p.opts); });
	}

	function loadCodeMirror(ed, opts) {
		if (isLoaded()) {
			addStyle(STYLE_ID, STYLE);
			attachCodeMirror(ed, opts);
			return;
		}
		pending.push({ed: ed, opts: opts});
		if (!requested) {
			requested = true;
			ed.fire(opts.libEvent, null, {toServer: true});
		}
	}

	/** Push the editor content to the textarea widget, so the server gets it before any other event. */
	function sync(ed) {
		if (ed._sqlqCM)
			ed._sqlqCM.save();
		ed.updateChange_();
	}

	function getSelection(ed) {
		if (ed._sqlqCM)
			return ed._sqlqCM.getSelection();
		var n = ed.getInputNode();
		return n.value.substring(n.selectionStart, n.selectionEnd);
	}

	function execute(ed, opts) {
		sync(ed);
		var data = {};
		data[opts.key] = getSelection(ed);
		ed.fire(opts.event, data, {toServer: true});
	}

	function insertIndent(n) {
		// execCommand keeps the browser undo history; setRangeText is the fallback
		if (!document.execCommand || !document.execCommand('insertText', false, INDENT))
			n.setRangeText(INDENT, n.selectionStart, n.selectionEnd, 'end');
	}

	function attachCodeMirror(ed, opts) {
		if (ed._sqlqCM || !ed.desktop)
			return;
		var n = ed.getInputNode();
		var run = function () { execute(ed, opts); };
		var cm = CodeMirror.fromTextArea(n, {
			mode: opts.mode,
			lineNumbers: true,
			indentUnit: INDENT.length,
			tabSize: INDENT.length,
			indentWithTabs: false,
			placeholder: n.placeholder,
			extraKeys: {
				'Ctrl-Enter': run,
				'Cmd-Enter': run,
				'Tab': function (cm) {
					if (cm.somethingSelected())
						cm.indentSelection('add');
					else
						cm.replaceSelection(INDENT, 'end');
				},
				'Shift-Tab': function (cm) { cm.indentSelection('subtract'); }
			}
		});
		ed._sqlqCM = cm;
		cm.getWrapperElement().classList.add('sqlq-editor');
		cm.setSize('100%', '100%');
		cm.on('blur', function () { sync(ed); });

		// server -> editor: setText() and focus() from the server must reach CodeMirror
		var setValue = ed.setValue;
		ed.setValue = function (value) {
			var result = setValue.apply(this, arguments);
			if (cm.getValue() !== (value || ''))
				cm.setValue(value || '');
			return result;
		};
		ed.focus = function () {
			cm.focus();
			return true;
		};

		// the holder gets its height from ZK flex; keep CodeMirror in sync when it changes
		var holder = cm.getWrapperElement().parentNode;
		if (window.ResizeObserver && holder)
			new ResizeObserver(function () { cm.refresh(); }).observe(holder);
		cm.refresh();
	}

	/**
	 * @param opts {editor: uuid, button: uuid, event: execute event name, key: selection data key,
	 *              codeEditor: false to keep the plain textarea, libEvent: event name to request
	 *              the CodeMirror library, mode: CodeMirror mime}
	 */
	function init(opts) {
		var ed = zk.Widget.$('#' + opts.editor);
		var btn = zk.Widget.$('#' + opts.button);
		if (!ed || !btn || !ed.desktop) {
			// forms are opened asynchronously: wait until the widgets are mounted (up to ~10s)
			opts.tries = (opts.tries || 0) + 1;
			if (opts.tries <= 50)
				setTimeout(function () { init(opts); }, 200);
			return;
		}
		if (ed._sqlqInit)
			return;
		ed._sqlqInit = true;

		// plain textarea shortcuts, also the fallback when CodeMirror can not be loaded
		ed.listen({onKeyDown: function (evt) {
			if (ed._sqlqCM)
				return;
			if ((evt.ctrlKey || evt.metaKey) && evt.keyCode == 13) {
				evt.stop();
				execute(ed, opts);
			} else if (evt.keyCode == 9 && !evt.shiftKey && !evt.ctrlKey && !evt.altKey && !evt.metaKey) {
				evt.stop();
				insertIndent(ed.getInputNode());
			}
		}});
		// execute button: same request as the keyboard, without the plain server onClick
		btn.listen({onClick: function (evt) {
			evt.stop({au: true});
			execute(ed, opts);
		}});

		if (opts.codeEditor)
			loadCodeMirror(ed, opts);
	}

	window.SQLQueryEnhanced = {init: init, libLoaded: libLoaded};
})();
