document.getElementById('filter').addEventListener('input', function (e) {
  var q = e.target.value.toLowerCase();
  document.querySelectorAll('section.type').forEach(function (s) {
    s.style.display = s.id.toLowerCase().indexOf(q) >= 0 ? '' : 'none';
  });
  document.querySelectorAll('nav.toc a').forEach(function (a) {
    a.style.display = a.textContent.toLowerCase().indexOf(q) >= 0 ? '' : 'none';
  });
});
