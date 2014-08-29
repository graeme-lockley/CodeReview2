$(document).ready(function () {
    $(function () {
        var UncheckedRevisions = Backbone.View.extend({
            el: "#thingy",
            initialize: function () {
            },
            render: function () {
                this.$el.html(_.template(templateOnName("portlets/unchecked-revisions/main.html"), {}));
                return this;
            }
        });

        var uncheckedRevisions = new UncheckedRevisions();
        uncheckedRevisions.render();
    });
});