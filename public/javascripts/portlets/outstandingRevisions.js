$(document).ready(function () {
    $(function () {
        var RevisionLineView = Backbone.View.extend({
            initialize: function () {
                this.$el = $("#revisionList-" + this.model.id)
            },
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
//            el: "#outstandingRevisions",
            initialize: function () {
                this.listenTo(this.model, 'sync', this.render);
                this.model.fetch();
            },
            render: function () {
                this.$el.html(_.template(templateOnName("portlets/outstandingRevisions/main.html"), {revisions: this.model}));

                this.model.forEach(function (revision) {
                    var x = new RevisionLineView({model: revision});
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