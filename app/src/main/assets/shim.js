/**
 * Zola Webview Injector
 * Hooks Zalo Web Webpack instance to extract conversation models and bridge to native Android UI.
 */
(function () {
  'use strict';
  if (window.__zw) return;

  var api = {};
  window.__zw = api;
  var cachedZalo = null;

  function findZalo() {
    if (cachedZalo) return cachedZalo;
    var jp = window.webpackJsonp;
    if (!jp || typeof jp.push !== 'function') return null;

    var reqInstance = null;
    var hookId = 'zola_' + Date.now();
    var mods = {};
    mods[hookId] = function (m, e, req) { reqInstance = req; };
    try {
      jp.push([[hookId], mods, [[hookId]]]);
    } catch (e) {
      return null;
    }

    if (!reqInstance || !reqInstance.c) return null;

    var found = {};
    for (var k in reqInstance.c) {
      var ex = reqInstance.c[k] && reqInstance.c[k].exports;
      if (!ex || typeof ex !== 'object') continue;
      if (!found.container && ex.ModuleContainer && typeof ex.ModuleContainer.resolve === 'function') found.container = ex.ModuleContainer;
      if (!found.tokens && ex.PreviewDataManager && ex.UnreadDataManager) found.tokens = ex;
      if (!found.convCtl && ex.ConversationController && ex.ConversationController.service) found.convCtl = ex.ConversationController;
      var def = ex.default;
      if (!found.storage && def && typeof def.getConversations === 'function') found.storage = def;
    }

    if (!found.container || !found.tokens || !found.storage) return null;

    var C = found.container;
    var T = found.tokens;
    function resolve(token) {
      try { return C.resolve(token); } catch (e) { return null; }
    }

    cachedZalo = {
      storage: found.storage,
      preview: resolve(T.PreviewDataManager),
      unread: resolve(T.UnreadDataManager),
      pin: resolve(T.PinDataManager),
      controller: found.convCtl ? resolve(found.convCtl) : null
    };
    return cachedZalo;
  }

  api.openConv = function (id) {
    var z = findZalo();
    if (z && z.controller && typeof z.controller.openConversation === 'function') {
      z.controller.openConversation(String(id));
      return true;
    }
    // Fallback: click DOM node if webpack module unavailable
    var row = document.querySelector('[data-id="' + id + '"]') || document.getElementById('conv_' + id);
    if (row) {
      row.click();
      return true;
    }
    return false;
  };

  function syncConversations() {
    var z = findZalo();
    if (!z || !window.ZaloBridge) return;

    z.storage.getConversations().then(function (convs) {
      if (!Array.isArray(convs)) return;

      var previews = {};
      var unreads = {};
      if (z.preview && z.preview.getAllPreviewsSync) {
        z.preview.getAllPreviewsSync().forEach(function (p) { if (p && p.convId) previews[p.convId] = p; });
      }
      if (z.unread && z.unread.getAllUnreadsSync) {
        z.unread.getAllUnreadsSync().forEach(function (u) { if (u && u.convId) unreads[u.convId] = u; });
      }

      var items = convs.map(function (c) {
        var id = String(c.userId || c.id || '');
        var p = previews[id] || {};
        var u = unreads[id] || {};

        var text = '';
        if (typeof p.plainText === 'string') text = p.plainText;
        else if (typeof p.preview === 'string') text = p.preview;
        else if (typeof p.message === 'string') text = p.message;

        return {
          id: id,
          name: c.displayName || c.zaloName || p.dName || 'User',
          isGroup: !!c.isGroup,
          avatar: c.avatar || '',
          lastMessage: text,
          time: p.messageTime ? +p.messageTime : 0,
          unread: (u.smsUnreadCount || 0) + (u.strangerUnreadCount || 0)
        };
      });

      window.ZaloBridge.onConvList(JSON.stringify(items));
    }).catch(function () {});
  }

  setInterval(syncConversations, 2500);
})();
