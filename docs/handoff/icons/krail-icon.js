// KRAIL Icon helper — exposes <span class="krail-icon" data-icon="search"></span>
// Auto-replaces all elements with [data-icon] on DOMContentLoaded
(function () {
  const icons = window.KrailIcons || {};
  function render(el) {
    const name = el.getAttribute("data-icon");
    const def = icons[name];
    if (!def) {
      el.innerHTML = '<svg viewBox="0 0 24 24"><rect width="24" height="24" fill="none" stroke="currentColor" stroke-dasharray="2 2"/></svg>';
      return;
    }
    el.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${def.vb}" width="100%" height="100%">${def.inner}</svg>`;
  }
  function renderAll(root) {
    (root || document).querySelectorAll("[data-icon]").forEach(render);
  }
  window.KrailIconRender = renderAll;
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => renderAll());
  } else {
    renderAll();
  }
})();
