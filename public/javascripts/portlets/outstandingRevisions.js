$(document).ready(function () {
    $(function () {
        var RevisionLineView = Backbone.View.extend({
            events: {
                "click": "clickButton"
            },
            clickButton: function (event) {
                console.log("/revisions/" + this.model.id + "/html");
                window.location = ("http://localhost:9000/revisions/" + this.model.id + "/html");
                return false;
            }
        });

        var OutstandingRevisionsView = Backbone.View.extend({
            initialize: function () {
                this.listenTo(this.model, 'sync', this.render);
                this.model.fetch();
            },
            render: function () {
                var prefix = this.$el.attr("id");
                this.$el.html(_.template(templateOnName("portlets/outstandingRevisions/main.html"), {revisions: this.model, prefix: prefix}));

                this.model.forEach(function (revision) {
                    var x = new RevisionLineView({model: revision, el: "#" + prefix + "-" + revision.id});
                });

                return this;
            }
        });

        $(".revisions").each(function (idx, dom) {
            var revisions = new Revisions();
            revisions.url = $(dom).attr("data-collection");
            var outstandingRevisionsView = new OutstandingRevisionsView({model: revisions, el: "#" + $(dom).attr("id")});
        });
    });
});