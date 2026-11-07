/*
 * Atlas Boutique (workshop 3): the only script of the application, plain JavaScript.
 * It is served from this server (the CSP forbids inline scripts) and is optional:
 * without it the shop still works, only the confirmations and the mobile menu are lost.
 *
 *  - confirmation before a destructive button ([data-confirm]);
 *  - mobile menu;
 *  - success and info messages disappear by themselves (errors stay).
 */
(function () {
  'use strict';

  // Confirmation: the click on a [data-confirm] button is cancelled unless the user agrees.
  document.addEventListener('click', function (e) {
    var cible = e.target.closest ? e.target.closest('[data-confirm]') : null;
    if (cible && !window.confirm(cible.getAttribute('data-confirm'))) {
      e.preventDefault();
    }
  });

  // Mobile menu
  var body = document.body;
  var scrim = document.querySelector('.scrim');
  function ouvrirMenu() {
    body.classList.add('menu-open');
    if (scrim) { scrim.classList.add('is-open'); }
  }
  function fermerMenu() {
    body.classList.remove('menu-open');
    if (scrim) { scrim.classList.remove('is-open'); }
  }
  document.addEventListener('click', function (e) {
    if (e.target.closest && e.target.closest('.js-menu')) { ouvrirMenu(); }
    else if (e.target.closest && e.target.closest('.js-menu-close')) { fermerMenu(); }
  });
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') { fermerMenu(); }
  });

  // Server messages: success and info fade out after 5 seconds.
  setTimeout(function () {
    var messages = document.querySelectorAll('.flash-success, .flash-info');
    Array.prototype.forEach.call(messages, function (m) {
      m.style.transition = 'opacity .4s';
      m.style.opacity = '0';
      setTimeout(function () { m.style.display = 'none'; }, 450);
    });
  }, 5000);
}());
